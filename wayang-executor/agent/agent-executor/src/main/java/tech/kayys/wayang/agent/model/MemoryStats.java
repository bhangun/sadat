package tech.kayys.wayang.agent.model;

import java.util.Map;

/**
 * Memory statistics
 */
public record MemoryStats(
        String sessionId,
        String tenantId,
        long messageCount,
        boolean cached) {

    public Map<String, Object> toMap() {
        return Map.of(
                "sessionId", sessionId,
                "tenantId", tenantId,
                "messageCount", messageCount,
                "cached", cached);
    }
}
