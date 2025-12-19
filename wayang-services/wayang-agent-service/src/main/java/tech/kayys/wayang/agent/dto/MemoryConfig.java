package tech.kayys.wayang.agent.dto;

public record MemoryConfig(
        boolean enabled,
        String type,
        int capacity,
        long retentionHours,
        boolean persist) {
}