package tech.kayys.wayang.agent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.SimilarMessage;
import tech.kayys.wayang.agent.dto.VectorStoreStats;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.VectorStore;

@ApplicationScoped
public class PostgresVectorStore implements VectorStore {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresVectorStore.class);

    @Inject
    io.vertx.mutiny.pgclient.PgPool pgPool;

    @Override
    public Uni<String> store(
            String sessionId,
            String tenantId,
            Message message,
            float[] embedding) {

        String id = UUID.randomUUID().toString();
        String vectorString = toVectorString(embedding);

        String sql = """
                INSERT INTO vector_memory (
                    id, session_id, tenant_id, role, content,
                    embedding, timestamp
                ) VALUES ($1, $2, $3, $4, $5, $6::vector, $7)
                """;
        LOG.debug("Storing vector in PostgreSQL: {}", sql);
        return pgPool.preparedQuery(sql)
                .execute(io.vertx.mutiny.sqlclient.Tuple.of(id)
                        .addValue(sessionId)
                        .addValue(tenantId)
                        .addValue(message.role())
                        .addValue(message.content())
                        .addValue(vectorString)
                        .addValue(message.timestamp()))
                .map(rowSet -> {
                    LOG.debug("Stored vector in PostgreSQL: {}", id);
                    return id;
                });
    }

    @Override
    public Uni<List<SimilarMessage>> search(
            String sessionId,
            String tenantId,
            float[] queryEmbedding,
            int limit) {

        String vectorString = toVectorString(queryEmbedding);

        String sql = """
                SELECT id, role, content, timestamp,
                       1 - (embedding <=> $1::vector) as similarity
                FROM vector_memory
                WHERE session_id = $2 AND tenant_id = $3
                ORDER BY embedding <=> $1::vector
                LIMIT $4
                """;

        return pgPool.preparedQuery(sql)
                .execute(io.vertx.mutiny.sqlclient.Tuple.of(
                        vectorString, sessionId, tenantId, limit))
                .map(rowSet -> {
                    List<SimilarMessage> results = new ArrayList<>();

                    rowSet.forEach(row -> {
                        Message message = new Message(
                                row.getString("role"),
                                row.getString("content"),
                                null,
                                null,
                                row.getLocalDateTime("timestamp")
                                        .atZone(java.time.ZoneOffset.UTC)
                                        .toInstant());

                        results.add(new SimilarMessage(
                                row.getString("id"),
                                message,
                                row.getDouble("similarity"),
                                Map.of()));
                    });

                    return results;
                });
    }

    @Override
    public Uni<List<SimilarMessage>> searchWithFilter(
            String sessionId,
            String tenantId,
            float[] queryEmbedding,
            Map<String, Object> filters,
            int limit) {

        // For pgvector, metadata filtering would require additional columns
        return search(sessionId, tenantId, queryEmbedding, limit);
    }

    @Override
    public Uni<Void> deleteSession(String sessionId, String tenantId) {
        String sql = "DELETE FROM vector_memory WHERE session_id = $1 AND tenant_id = $2";

        return pgPool.preparedQuery(sql)
                .execute(io.vertx.mutiny.sqlclient.Tuple.of(sessionId, tenantId))
                .replaceWithVoid();
    }

    @Override
    public Uni<VectorStoreStats> getStats(String tenantId) {
        String sql = """
                SELECT COUNT(*) as total_vectors
                FROM vector_memory
                WHERE tenant_id = $1
                """;

        return pgPool.preparedQuery(sql)
                .execute(io.vertx.mutiny.sqlclient.Tuple.of(tenantId))
                .map(rowSet -> {
                    long total = rowSet.iterator().next().getLong("total_vectors");
                    return new VectorStoreStats(total, 1536, "pgvector");
                });
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
