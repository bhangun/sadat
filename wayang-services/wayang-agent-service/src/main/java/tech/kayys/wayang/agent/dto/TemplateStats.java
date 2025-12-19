package tech.kayys.wayang.agent.dto;

import java.time.Instant;

/**
 * Template statistics
 */
public record TemplateStats(
        String templateId,
        long instantiations,
        long activeAgents,
        double averageRating,
        Instant lastUsed) {
}