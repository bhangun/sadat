package tech.kayys.wayang.rag.repository;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class VectorRepository {
    
    @Inject
    PgPool client;
    
    public Uni<String> store(VectorRecord record) {
        return client.preparedQuery("""
            INSERT INTO embeddings (
                tenant_id, document_id, chunk_id, text, embedding, metadata
            ) VALUES ($1, $2, $3, $4, $5::vector, $6::jsonb)
            RETURNING id
            """)
            .execute(Tuple.of(
                record.tenantId(),
                record.documentId(),
                record.chunkId(),
                record.text(),
                formatVector(record.embedding()),
                JsonUtil.toJson(record.metadata())
            ))
            .map(rows -> rows.iterator().next().getString("id"));
    }
    
    public Uni<List<VectorRecord>> search(VectorSearchRequest request) {
        String sql = """
            SELECT 
                id, tenant_id, document_id, chunk_id, text, metadata,
                embedding <=> $1::vector AS distance
            FROM embeddings
            WHERE tenant_id = $2
            %s
            ORDER BY embedding <=> $1::vector
            LIMIT $3
            """.formatted(buildFilterClause(request.filters()));
        
        return client.preparedQuery(sql)
            .execute(Tuple.of(
                formatVector(request.queryEmbedding()),
                request.tenantId(),
                request.topK()
            ))
            .map(rows -> {
                List<VectorRecord> results = new ArrayList<>();
                for (Row row : rows) {
                    results.add(VectorRecord.from(row));
                }
                return results;
            });
    }
    
    public Uni<List<VectorRecord>> keywordSearch(
        String tenantId, 
        String query, 
        int limit
    ) {
        return client.preparedQuery("""
            SELECT 
                id, tenant_id, document_id, chunk_id, text, metadata,
                ts_rank(to_tsvector('english', text), plainto_tsquery('english', $1)) AS rank
            FROM embeddings
            WHERE tenant_id = $2
              AND to_tsvector('english', text) @@ plainto_tsquery('english', $1)
            ORDER BY rank DESC
            LIMIT $3
            """)
            .execute(Tuple.of(query, tenantId, limit))
            .map(rows -> {
                List<VectorRecord> results = new ArrayList<>();
                for (Row row : rows) {
                    results.add(VectorRecord.from(row));
                }
                return results;
            });
    }
    
    private String formatVector(float[] embedding) {
        return "[" + Arrays.stream(embedding)
            .mapToObj(String::valueOf)
            .collect(Collectors.joining(",")) + "]";
    }
    
    private String buildFilterClause(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        
        return "AND " + filters.entrySet().stream()
            .map(e -> "metadata->>'%s' = '%s'".formatted(e.getKey(), e.getValue()))
            .collect(Collectors.joining(" AND "));
    }
}