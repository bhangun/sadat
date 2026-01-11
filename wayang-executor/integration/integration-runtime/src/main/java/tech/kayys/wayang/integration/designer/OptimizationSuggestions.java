package tech.kayys.wayang.integration.designer;

import java.time.Instant;
import java.util.List;

record OptimizationSuggestions(
    String routeId,
    List<OptimizationSuggestion> suggestions,
    double estimatedImprovement,
    Instant analyzedAt
) {}