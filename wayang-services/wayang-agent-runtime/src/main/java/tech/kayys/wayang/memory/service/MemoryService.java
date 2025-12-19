package tech.kayys.wayang.memory.service;

/**
 * Memory Service
 */

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.model.MemoryBackend;
import tech.kayys.wayang.model.MemoryConfig;

import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class MemoryService {

    private static final Logger LOG = Logger.getLogger(MemoryService.class);

    private final Map<MemoryConfig.StorageBackend, MemoryBackend> backends = new HashMap<>();

    @Inject
    public MemoryService(Instance<MemoryBackend> backendInstances) {
        backendInstances.forEach(backend -> backends.put(backend.getSupportedBackend(), backend));
    }

    /**
     * Store message in memory
     */
    public Uni<Void> storeMessage(String sessionId, String role, String content,
            MemoryConfig config) {
        if (config == null || config.getType() == MemoryConfig.MemoryType.NONE) {
            return Uni.createFrom().voidItem();
        }

        MemoryBackend backend = getBackend(config.getStorageBackend());
        MemoryBackend.MemoryEntry entry = new MemoryBackend.MemoryEntry(sessionId, role, content);

        return backend.store(sessionId, entry)
                .invoke(() -> LOG.debugf("Stored message in memory: %s", sessionId));
    }

    /**
     * Retrieve conversation history
     */
    public Uni<List<MemoryBackend.MemoryEntry>> getHistory(String sessionId, MemoryConfig config) {
        if (config == null || config.getType() == MemoryConfig.MemoryType.NONE) {
            return Uni.createFrom().item(List.of());
        }

        MemoryBackend backend = getBackend(config.getStorageBackend());
        int limit = getMemoryLimit(config);

        return backend.retrieve(sessionId, limit);
    }

    /**
     * Search memory
     */
    public Uni<List<MemoryBackend.MemoryEntry>> searchMemory(String sessionId, String query,
            MemoryConfig config) {
        if (config == null || config.getType() == MemoryConfig.MemoryType.NONE) {
            return Uni.createFrom().item(List.of());
        }

        MemoryBackend backend = getBackend(config.getStorageBackend());
        return backend.search(sessionId, query, 10);
    }

    /**
     * Clear memory
     */
    public Uni<Void> clearMemory(String sessionId, MemoryConfig config) {
        if (config == null || config.getType() == MemoryConfig.MemoryType.NONE) {
            return Uni.createFrom().voidItem();
        }

        MemoryBackend backend = getBackend(config.getStorageBackend());
        return backend.clear(sessionId);
    }

    /**
     * Format conversation history for LLM context
     */
    public Uni<String> formatHistory(String sessionId, MemoryConfig config) {
        return getHistory(sessionId, config)
                .map(entries -> {
                    if (entries.isEmpty()) {
                        return "";
                    }

                    StringBuilder formatted = new StringBuilder();
                    formatted.append("Previous conversation:\n");

                    entries.forEach(entry -> {
                        formatted.append(entry.getRole()).append(": ")
                                .append(entry.getContent()).append("\n");
                    });

                    return formatted.toString();
                });
    }

    /**
     * Summarize long conversation history
     */
    public Uni<String> summarizeHistory(String sessionId, MemoryConfig config,
            io.quarkus.ai.agent.runtime.service.LLMService llmService,
            io.quarkus.ai.agent.runtime.model.LLMConfig llmConfig) {

        if (config.getType() != MemoryConfig.MemoryType.SUMMARY) {
            return Uni.createFrom().item("");
        }

        return getHistory(sessionId, config)
                .chain(entries -> {
                    if (entries.isEmpty()) {
                        return Uni.createFrom().item("");
                    }

                    // Build conversation text
                    String conversation = entries.stream()
                            .map(e -> e.getRole() + ": " + e.getContent())
                            .collect(Collectors.joining("\n"));

                    // Ask LLM to summarize
                    String prompt = "Summarize this conversation concisely:\n\n" + conversation;

                    return llmService.complete(llmConfig, prompt,
                            new io.quarkus.ai.agent.runtime.context.ExecutionContext());
                });
    }

    private MemoryBackend getBackend(MemoryConfig.StorageBackend backendType) {
        MemoryBackend backend = backends.get(backendType);
        if (backend == null) {
            LOG.warnf("Backend not found: %s, using in-memory", backendType);
            return backends.get(MemoryConfig.StorageBackend.IN_MEMORY);
        }
        return backend;
    }

    private int getMemoryLimit(MemoryConfig config) {
        if (config.getConfig() != null && config.getConfig().containsKey("maxMessages")) {
            return (int) config.getConfig().get("maxMessages");
        }

        // Default limits based on memory type
        return switch (config.getType()) {
            case BUFFER -> 50;
            case WINDOW -> 10;
            case SUMMARY -> 100;
            default -> 20;
        };
    }
}