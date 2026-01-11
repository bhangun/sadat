package tech.kayys.wayang.agent.model;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.service.MemoryStorageService;

@ApplicationScoped
public class DefaultAgentMemoryManager implements AgentMemoryManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAgentMemoryManager.class);

    @Inject
    MemoryStorageService storageService;

    @Inject
    MemoryStrategyFactory strategyFactory;

    @Inject
    MemoryCache memoryCache;

    @Override
    public Uni<List<Message>> loadMemory(
            String sessionId,
            String tenantId,
            String memoryType,
            Integer windowSize) {

        LOG.debug("Loading memory: session={}, type={}, window={}",
                sessionId, memoryType, windowSize);

        // Try cache first
        return memoryCache.get(sessionId, tenantId)
                .flatMap(cached -> {
                    if (cached != null && !cached.isEmpty()) {
                        LOG.debug("Memory loaded from cache: {} messages", cached.size());
                        return Uni.createFrom().item(cached);
                    }

                    // Load from storage
                    return loadFromStorage(sessionId, tenantId, memoryType, windowSize);
                });
    }

    private Uni<List<Message>> loadFromStorage(
            String sessionId,
            String tenantId,
            String memoryType,
            Integer windowSize) {

        return strategyFactory.getStrategy(memoryType)
                .flatMap(strategy -> storageService.loadMessages(sessionId, tenantId)
                        .map(messages -> strategy.process(messages, windowSize)))
                .onItem().invoke(messages -> {
                    // Update cache
                    memoryCache.put(sessionId, tenantId, messages);
                    LOG.debug("Memory loaded from storage: {} messages", messages.size());
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.error("Failed to load memory: {}", error.getMessage());
                    return List.of();
                });
    }

    @Override
    public Uni<Void> saveMessages(
            String sessionId,
            String tenantId,
            List<Message> messages) {

        if (messages == null || messages.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        LOG.debug("Saving {} messages for session: {}", messages.size(), sessionId);

        return storageService.saveMessages(sessionId, tenantId, messages)
                .onItem().invoke(v -> {
                    // Update cache
                    memoryCache.append(sessionId, tenantId, messages);
                    LOG.debug("Messages saved successfully");
                })
                .onFailure().invoke(error -> LOG.error("Failed to save messages: {}", error.getMessage(), error));
    }

    @Override
    public Uni<Void> clearMemory(String sessionId, String tenantId) {
        LOG.info("Clearing memory for session: {}", sessionId);

        return storageService.clearMessages(sessionId, tenantId)
                .onItem().invoke(v -> memoryCache.invalidate(sessionId, tenantId));
    }

    @Override
    public Uni<List<Message>> searchMemory(
            String sessionId,
            String tenantId,
            String query,
            int limit) {

        LOG.debug("Searching memory: session={}, query={}", sessionId, query);

        return storageService.searchMessages(sessionId, tenantId, query, limit);
    }

    @Override
    public Uni<MemoryStats> getStats(String sessionId, String tenantId) {
        return storageService.getMessageCount(sessionId, tenantId)
                .map(count -> new MemoryStats(
                        sessionId,
                        tenantId,
                        count,
                        memoryCache.isCached(sessionId, tenantId)));
    }
}
