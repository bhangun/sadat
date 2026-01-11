package tech.kayys.wayang.agent.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.repository.MessageRepository;

/**
 * Service for persisting agent memory and messages
 */
@ApplicationScoped
public class MemoryStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryStorageService.class);

    @Inject
    MessageRepository messageRepository;

    /**
     * Load messages for a session
     */
    public Uni<List<Message>> loadMessages(String sessionId, String tenantId) {
        LOG.debug("Loading messages: session={}, tenant={}", sessionId, tenantId);

        return messageRepository.findBySession(sessionId, tenantId)
                .onItem().invoke(messages -> LOG.debug("Loaded {} messages", messages.size()))
                .onFailure().invoke(error -> LOG.error("Failed to load messages: {}", error.getMessage()));
    }

    /**
     * Save messages
     */
    public Uni<Void> saveMessages(
            String sessionId,
            String tenantId,
            List<Message> messages) {

        LOG.debug("Saving {} messages for session: {}", messages.size(), sessionId);

        return messageRepository.save(sessionId, tenantId, messages)
                .onItem().invoke(v -> LOG.debug("Messages saved successfully"))
                .onFailure().invoke(error -> LOG.error("Failed to save messages: {}", error.getMessage()));
    }

    /**
     * Clear messages for a session
     */
    public Uni<Void> clearMessages(String sessionId, String tenantId) {
        LOG.info("Clearing messages: session={}", sessionId);

        return messageRepository.deleteBySession(sessionId, tenantId);
    }

    /**
     * Search messages semantically
     */
    public Uni<List<Message>> searchMessages(
            String sessionId,
            String tenantId,
            String query,
            int limit) {

        LOG.debug("Searching messages: query={}, limit={}", query, limit);

        return messageRepository.search(sessionId, tenantId, query, limit);
    }

    /**
     * Get message count for a session
     */
    public Uni<Long> getMessageCount(String sessionId, String tenantId) {
        return messageRepository.count(sessionId, tenantId);
    }
}
