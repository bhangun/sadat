package tech.kayys.silat.executor.camel.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record ImageClassificationResult(
    String label,
    double confidence,
    List<Map<String, Object>> topPredictions,
    Instant classifiedAt
) {}