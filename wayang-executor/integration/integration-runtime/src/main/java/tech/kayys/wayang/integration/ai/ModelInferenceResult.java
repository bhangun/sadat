package tech.kayys.silat.executor.camel.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record ModelInferenceResult(
    String modelName,
    String framework,
    List<Double> predictions,
    Map<String, Object> metadata,
    Instant inferredAt
) {}