package tech.kayys.wayang.agent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.SimilarMessage;
import tech.kayys.wayang.agent.model.MemoryStrategy;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.VectorStore;

@ApplicationScoped
public class ProductionVectorMemoryStrategy implements
        MemoryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ProductionVectorMemoryStrategy.class);

    @Inject
    VectorStore vectorStore;

    @Inject
    EmbeddingService embeddingService;

    @Override
    public List<Message> process(List<Message> messages, Integer windowSize) {
        // Vector memory retrieval happens via search, not process
        // Return recent messages as fallback
        int window = windowSize != null ? windowSize : 10;
        int startIndex = Math.max(0, messages.size() - window);
        return new ArrayList<>(messages.subList(startIndex, messages.size()));
    }

    @Override
    public String getType() {
        return "vector";
    }

    /**
     * Search semantic memory
     */
    public Uni<List<Message>> semanticSearch(
            String sessionId,
            String tenantId,
            String query,
            int limit) {

        LOG.debug("Semantic search: query='{}', limit={}", query, limit);

        return embeddingService.generateEmbedding(query)
                .flatMap(embedding -> vectorStore.search(sessionId, tenantId, embedding, limit))
                .map(results -> results.stream()
                        .map(SimilarMessage::message)
                        .collect(Collectors.toList()));
    }

    /**
     * Store messages with embeddings
     */
    public Uni<Void> storeMessages(
            String sessionId,
            String tenantId,
            List<Message> messages) {

        // Generate embeddings for all messages
        List<String> texts = messages.stream()
                .map(Message::content)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return embeddingService.generateEmbeddings(texts)
                .flatMap(embeddings -> {
                    List<Uni<String>> storeOps = new ArrayList<>();

                    for (int i = 0; i < messages.size(); i++) {
                        Message msg = messages.get(i);
                        float[] embedding = embeddings.get(i);

                        storeOps.add(vectorStore.store(
                                sessionId, tenantId, msg, embedding));
                    }

                    return Uni.combine().all().unis(storeOps).with(
                            results -> null);
                })
                .replaceWithVoid();
    }
}