package tech.kayys.wayang.agent.dto;

import java.util.Map;

import tech.kayys.wayang.agent.PlanningStrategy;

/**
 * Plan Metadata
 */
public record PlanMetadata(
    PlanningStrategy strategy,
    int totalSteps,
    int estimatedExecutionTimeMs,
    double confidenceScore,
    Map<String, String> tags
) {}
