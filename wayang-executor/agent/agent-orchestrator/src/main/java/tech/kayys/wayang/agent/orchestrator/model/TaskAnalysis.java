package tech.kayys.wayang.agent.orchestrator.model;

import java.util.Map;
import java.util.Set;

record TaskAnalysis(
    TaskComplexity complexity,
    Set<String> requiredSkills,
    int estimatedSteps,
    boolean requiresSpecialization,
    Map<String, Object> metadata
) {}