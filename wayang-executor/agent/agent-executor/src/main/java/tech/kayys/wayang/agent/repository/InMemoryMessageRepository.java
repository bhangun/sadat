package tech.kayys.wayang.agent.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Message;

/**
 * In-memory implementation of MessageRepository
 * For production, replace with actual database implementation
 */
@ApplicationScoped
public class InMemoryMessageRepository implements MessageRepository {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryMessageRepository.class);

    // Map: sessionKey -> List<Message>
    private final Map<String, List<Message>> storage = new ConcurrentHashMap<>();

    @Override
    public Uni<List<Message>> findBySession(String sessionId, String tenantId) {
        String key = makeKey(sessionId, tenantId);
        List<Message> messages = storage.getOrDefault(key, List.of());
        return Uni.createFrom().item(new ArrayList<>(messages));
    }

    @Override
    public Uni<Void> save(String sessionId, String tenantId, List<Message> messages) {
        String key = makeKey(sessionId, tenantId);

        // Append to existing messages
        storage.compute(key, (k, existing) -> {
            if (existing == null) {
                return new ArrayList<>(messages);
            } else {
                List<Message> updated = new ArrayList<>(existing);
                updated.addAll(messages);
                return updated;
            }
        });

        LOG.debug("Saved {} messages for session: {}", messages.size(), key);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> deleteBySession(String sessionId, String tenantId) {
        String key = makeKey(sessionId, tenantId);
        storage.remove(key);
        LOG.debug("Deleted messages for session: {}", key);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<List<Message>> search(String sessionId, String tenantId, String query, int limit) {
        // Simple substring search for in-memory implementation
        // In production, use proper vector search
        String key = makeKey(sessionId, tenantId);
        List<Message> messages = storage.getOrDefault(key, List.of());

        List<Message> results = messages.stream()
                .filter(msg -> msg.content() != null &&
                        msg.content().toLowerCase().contains(query.toLowerCase()))
                .limit(limit)
                .toList();

        return Uni.createFrom().item(results);
    }

    @Override
    public Uni<Long> count(String sessionId, String tenantId) {
        String key = makeKey(sessionId, tenantId);
        List<Message> messages = storage.getOrDefault(key, List.of());
        return Uni.createFrom().item((long) messages.size());
    }

    private String makeKey(String sessionId, String tenantId) {
        return tenantId + ":" + sessionId;
    }
}
