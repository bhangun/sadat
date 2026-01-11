package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.dto.SimilarMessage;
import tech.kayys.wayang.agent.dto.VectorStoreStats;

/**
 * Interface for vector database operations
 */
public interface VectorStore {

    /**
     * Store message with embedding
     */
    Uni<String> store(
            String sessionId,
            String tenantId,
            Message message,
            float[] embedding);

    /**
     * Search similar messages
     */
    Uni<List<SimilarMessage>> search(
            String sessionId,
            String tenantId,
            float[] queryEmbedding,
            int limit);

    /**
     * Search with metadata filters
     */
    Uni<List<SimilarMessage>> searchWithFilter(
            String sessionId,
            String tenantId,
            float[] queryEmbedding,
            Map<String, Object> filters,
            int limit);

    /**
     * Delete messages for session
     */
    Uni<Void> deleteSession(String sessionId, String tenantId);

    /**
     * Get statistics
     */
    Uni<VectorStoreStats> getStats(String tenantId);
}
