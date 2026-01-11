package tech.kayys.wayang.integration.designer;

import java.util.Map;

record OptimizationSuggestion(
    String category,
    String type,
    String message,
    String nodeId,
    Map<String, Object> suggestedChanges,
    int priority
) {}