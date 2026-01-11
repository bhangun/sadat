package tech.kayys.silat.executor.memory;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Production-grade vector memory store using PostgreSQL with pgvector extension.
 */
@ApplicationScoped
public class PostgresVectorStore implements VectorMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresVectorStore.class);

    @Inject
    PgPool pgPool;

    @ConfigProperty(name = "silat.memory.vector.dimension", defaultValue = "1536")
    int vectorDimension;

    @ConfigProperty(name = "silat.memory.index.type", defaultValue = "hnsw")
    String indexType; // hnsw or ivfflat

    /**
     * Initialize database schema
     */
    public Uni<Void> initialize() {
        LOG.info("Initializing PostgreSQL vector store");

        String createTableSql = """
            CREATE TABLE IF NOT EXISTS silat_memories (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                namespace VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                content_tsvector tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
                embedding vector(%d),
                type VARCHAR(50) NOT NULL,
                metadata JSONB DEFAULT '{}'::jsonb,
                timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                expires_at TIMESTAMPTZ,
                importance DOUBLE PRECISION NOT NULL DEFAULT 0.5,
                tenant_id VARCHAR(255) NOT NULL,
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );

            CREATE INDEX IF NOT EXISTS idx_memories_namespace ON silat_memories(namespace);
            CREATE INDEX IF NOT EXISTS idx_memories_tenant ON silat_memories(tenant_id);
            CREATE INDEX IF NOT EXISTS idx_memories_type ON silat_memories(type);
            CREATE INDEX IF NOT EXISTS idx_memories_timestamp ON silat_memories(timestamp DESC);
            CREATE INDEX IF NOT EXISTS idx_memories_importance ON silat_memories(importance DESC);
            CREATE INDEX IF NOT EXISTS idx_memories_content_fts ON silat_memories USING GIN(content_tsvector);
            CREATE INDEX IF NOT EXISTS idx_memories_metadata ON silat_memories USING GIN(metadata);

            -- Vector similarity index (HNSW for better performance)
            CREATE INDEX IF NOT EXISTS idx_memories_embedding_hnsw
                ON silat_memories USING hnsw (embedding vector_cosine_ops)
                WITH (m = 16, ef_construction = 64);
            """.formatted(vectorDimension);

        return pgPool.query(createTableSql)
            .execute()
            .replaceWithVoid()
            .invoke(() -> LOG.info("Vector store initialized"));
    }

    @Override
    public Uni<String> store(Memory memory) {
        LOG.debug("Storing memory: {}", memory.getId());

        String sql = """
            INSERT INTO silat_memories (
                id, namespace, content, embedding, type, metadata,
                timestamp, expires_at, importance, tenant_id
            ) VALUES ($1, $2, $3, $4::vector, $5, $6::jsonb, $7, $8, $9, $10)
            ON CONFLICT (id) DO UPDATE SET
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                metadata = EXCLUDED.metadata,
                importance = EXCLUDED.importance,
                updated_at = NOW()
            RETURNING id
            """;

        Tuple params = Tuple.of(
            UUID.fromString(memory.getId()),
            memory.getNamespace(),
            memory.getContent(),
            vectorToString(memory.getEmbedding()),
            memory.getType().name(),
            toJsonb(memory.getMetadata()),
            memory.getTimestamp(),
            memory.getExpiresAt(),
            memory.getImportance(),
            extractTenantId(memory.getNamespace())
        );

        return pgPool.preparedQuery(sql)
            .execute(params)
            .map(rowSet -> {
                Row row = rowSet.iterator().next();
                return row.getUUID("id").toString();
            });
    }

    @Override
    public Uni<List<String>> storeBatch(List<Memory> memories) {
        LOG.debug("Storing batch of {} memories", memories.size());

        return Panache.withTransaction(() -> {
            List<Uni<String>> stores = memories.stream()
                .map(this::store)
                .toList();

            return Uni.join().all(stores).andFailFast();
        });
    }

    @Override
    public Uni<List<ScoredMemory>> search(
            float[] queryEmbedding,
            int limit,
            double minSimilarity,
            Map<String, Object> filters) {

        LOG.debug("Vector search with limit: {}, minSimilarity: {}", limit, minSimilarity);

        // Build dynamic query based on filters
        StringBuilder sql = new StringBuilder("""
            SELECT
                id, namespace, content, embedding::text, type,
                metadata, timestamp, expires_at, importance,
                1 - (embedding <=> $1::vector) as similarity
            FROM silat_memories
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();
        params.add(vectorToString(queryEmbedding));
        int paramIndex = 2;

        // Add filters
        if (filters != null) {
            if (filters.containsKey("namespace")) {
                sql.append(" AND namespace = $").append(paramIndex++);
                params.add(filters.get("namespace"));
            }

            if (filters.containsKey("types")) {
                sql.append(" AND type = ANY($").append(paramIndex++).append(")");
                List<String> types = ((List<?>) filters.get("types")).stream()
                    .map(Object::toString)
                    .toList();
                params.add(types.toArray(new String[0]));
            }

            if (filters.containsKey("minImportance")) {
                sql.append(" AND importance >= $").append(paramIndex++);
                params.add(filters.get("minImportance"));
            }
        }

        // Exclude expired
        sql.append(" AND (expires_at IS NULL OR expires_at > NOW())");

        // Similarity threshold
        sql.append(" AND 1 - (embedding <=> $1::vector) >= $").append(paramIndex++);
        params.add(minSimilarity);

        // Order and limit
        sql.append(" ORDER BY embedding <=> $1::vector ASC LIMIT $").append(paramIndex);
        params.add(limit);

        return pgPool.preparedQuery(sql.toString())
            .execute(Tuple.wrap(params))
            .map(rowSet -> {
                List<ScoredMemory> results = new ArrayList<>();

                for (Row row : rowSet) {
                    Memory memory = rowToMemory(row);
                    double similarity = row.getDouble("similarity");
                    results.add(new ScoredMemory(memory, similarity));
                }

                LOG.debug("Found {} memories", results.size());
                return results;
            });
    }

    @Override
    public Uni<List<ScoredMemory>> hybridSearch(
            float[] queryEmbedding,
            List<String> keywords,
            int limit,
            double semanticWeight) {

        LOG.debug("Hybrid search with {} keywords, semantic weight: {}",
            keywords.size(), semanticWeight);

        String keywordQuery = String.join(" | ", keywords);
        double keywordWeight = 1.0 - semanticWeight;

        String sql = """
            WITH semantic_results AS (
                SELECT
                    id, namespace, content, embedding::text, type,
                    metadata, timestamp, expires_at, importance,
                    1 - (embedding <=> $1::vector) as semantic_score
                FROM silat_memories
                WHERE (expires_at IS NULL OR expires_at > NOW())
                    AND namespace = $2
            ),
            keyword_results AS (
                SELECT
                    id,
                    ts_rank(content_tsvector, to_tsquery('english', $3)) as keyword_score
                FROM silat_memories
                WHERE content_tsvector @@ to_tsquery('english', $3)
                    AND (expires_at IS NULL OR expires_at > NOW())
                    AND namespace = $2
            )
            SELECT
                s.id, s.namespace, s.content, s.embedding, s.type,
                s.metadata, s.timestamp, s.expires_at, s.importance,
                (s.semantic_score * $4 + COALESCE(k.keyword_score, 0) * $5) as combined_score,
                s.semantic_score,
                COALESCE(k.keyword_score, 0) as keyword_score
            FROM semantic_results s
            LEFT JOIN keyword_results k ON s.id = k.id
            ORDER BY combined_score DESC
            LIMIT $6
            """;

        String namespace = "default"; // From filters if available

        Tuple params = Tuple.of(
            vectorToString(queryEmbedding),
            namespace,
            keywordQuery,
            semanticWeight,
            keywordWeight,
            limit
        );

        return pgPool.preparedQuery(sql)
            .execute(params)
            .map(rowSet -> {
                List<ScoredMemory> results = new ArrayList<>();

                for (Row row : rowSet) {
                    Memory memory = rowToMemory(row);
                    double combinedScore = row.getDouble("combined_score");

                    Map<String, Object> scoreBreakdown = Map.of(
                        "total", combinedScore,
                        "semantic", row.getDouble("semantic_score"),
                        "keyword", row.getDouble("keyword_score")
                    );

                    results.add(new ScoredMemory(memory, combinedScore, scoreBreakdown));
                }

                return results;
            });
    }

    @Override
    public Uni<Memory> retrieve(String memoryId) {
        LOG.debug("Retrieving memory: {}", memoryId);

        String sql = """
            SELECT id, namespace, content, embedding::text, type,
                   metadata, timestamp, expires_at, importance
            FROM silat_memories
            WHERE id = $1
                AND (expires_at IS NULL OR expires_at > NOW())
            """;

        return pgPool.preparedQuery(sql)
            .execute(Tuple.of(UUID.fromString(memoryId)))
            .map(rowSet -> {
                if (!rowSet.iterator().hasNext()) {
                    return null;
                }
                return rowToMemory(rowSet.iterator().next());
            });
    }

    @Override
    public Uni<List<Memory>> retrieveBatch(List<String> memoryIds) {
        LOG.debug("Retrieving batch of {} memories", memoryIds.size());

        String sql = """
            SELECT id, namespace, content, embedding::text, type,
                   metadata, timestamp, expires_at, importance
            FROM silat_memories
            WHERE id = ANY($1)
                AND (expires_at IS NULL OR expires_at > NOW())
            """;

        UUID[] uuids = memoryIds.stream()
            .map(UUID::fromString)
            .toArray(UUID[]::new);

        return pgPool.preparedQuery(sql)
            .execute(Tuple.of((Object) uuids))
            .map(rowSet -> {
                List<Memory> memories = new ArrayList<>();
                for (Row row : rowSet) {
                    memories.add(rowToMemory(row));
                }
                return memories;
            });
    }

    @Override
    public Uni<Memory> updateMetadata(String memoryId, Map<String, Object> metadata) {
        LOG.debug("Updating metadata for memory: {}", memoryId);

        String sql = """
            UPDATE silat_memories
            SET metadata = metadata || $1::jsonb,
                updated_at = NOW()
            WHERE id = $2
            RETURNING id, namespace, content, embedding::text, type,
                      metadata, timestamp, expires_at, importance
            """;

        return pgPool.preparedQuery(sql)
            .execute(Tuple.of(toJsonb(metadata), UUID.fromString(memoryId)))
            .map(rowSet -> {
                if (!rowSet.iterator().hasNext()) {
                    return null;
                }
                return rowToMemory(rowSet.iterator().next());
            });
    }

    @Override
    public Uni<Boolean> delete(String memoryId) {
        LOG.debug("Deleting memory: {}", memoryId);

        String sql = "DELETE FROM silat_memories WHERE id = $1";

        return pgPool.preparedQuery(sql)
            .execute(Tuple.of(UUID.fromString(memoryId)))
            .map(rowSet -> rowSet.rowCount() > 0);
    }

    @Override
    public Uni<Long> deleteNamespace(String namespace) {
        LOG.info("Deleting all memories in namespace: {}", namespace);

        String sql = "DELETE FROM silat_memories WHERE namespace = $1";

        return pgPool.preparedQuery(sql)
            .execute(Tuple.of(namespace))
            .map(rowSet -> (long) rowSet.rowCount());
    }

    @Override
    public Uni<MemoryStatistics> getStatistics(String namespace) {
        LOG.debug("Getting statistics for namespace: {}", namespace);

        String sql = """
            SELECT
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE type = 'EPISODIC') as episodic,
                COUNT(*) FILTER (WHERE type = 'SEMANTIC') as semantic,
                COUNT(*) FILTER (WHERE type = 'PROCEDURAL') as procedural,
                COUNT(*) FILTER (WHERE type = 'WORKING') as working,
                AVG(importance) as avg_importance,
                MIN(timestamp) as oldest,
                MAX(timestamp) as newest
            FROM silat_memories
            WHERE namespace = $1
                AND (expires_at IS NULL OR expires_at > NOW())
            """;

        return pgPool.preparedQuery(sql)
            .execute(Tuple.of(namespace))
            .map(rowSet -> {
                if (!rowSet.iterator().hasNext()) {
                    return new MemoryStatistics(namespace, 0, 0, 0, 0, 0, 0.0, null, null);
                }

                Row row = rowSet.iterator().next();

                return new MemoryStatistics(
                    namespace,
                    row.getLong("total"),
                    row.getLong("episodic"),
                    row.getLong("semantic"),
                    row.getLong("procedural"),
                    row.getLong("working"),
                    row.getDouble("avg_importance"),
                    row.getLocalDateTime("oldest") != null ?
                        Instant.from(row.getLocalDateTime("oldest")) : null,
                    row.getLocalDateTime("newest") != null ?
                        Instant.from(row.getLocalDateTime("newest")) : null
                );
            });
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Convert row to Memory object
     */
    private Memory rowToMemory(Row row) {
        return Memory.builder()
            .id(row.getUUID("id").toString())
            .namespace(row.getString("namespace"))
            .content(row.getString("content"))
            .embedding(stringToVector(row.getString("embedding")))
            .type(MemoryType.valueOf(row.getString("type")))
            .metadata(fromJsonb(row.getString("metadata")))
            .timestamp(Instant.from(row.getLocalDateTime("timestamp")))
            .expiresAt(row.getLocalDateTime("expires_at") != null ?
                Instant.from(row.getLocalDateTime("expires_at")) : null)
            .importance(row.getDouble("importance"))
            .build();
    }

    /**
     * Convert float array to PostgreSQL vector format
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Convert PostgreSQL vector string to float array
     */
    private float[] stringToVector(String vectorStr) {
        // Remove brackets and split
        String cleaned = vectorStr.substring(1, vectorStr.length() - 1);
        String[] parts = cleaned.split(",");

        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }

        return vector;
    }

    /**
     * Convert map to JSONB string
     */
    private String toJsonb(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(map);
        } catch (Exception e) {
            LOG.error("Failed to convert map to JSONB", e);
            return "{}";
        }
    }

    /**
     * Convert JSONB string to map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJsonb(String jsonb) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(jsonb, Map.class);
        } catch (Exception e) {
            LOG.error("Failed to parse JSONB", e);
            return new HashMap<>();
        }
    }

    /**
     * Extract tenant ID from namespace
     */
    private String extractTenantId(String namespace) {
        // Namespace format: tenant:workflow:node
        String[] parts = namespace.split(":");
        return parts.length > 0 ? parts[0] : "default";
    }
}