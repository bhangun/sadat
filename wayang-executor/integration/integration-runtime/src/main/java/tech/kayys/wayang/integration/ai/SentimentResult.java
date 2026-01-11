package tech.kayys.silat.executor.camel.ai;

import java.time.Instant;
import java.util.Map;

record SentimentResult(
    String sentiment,
    double confidence,
    Map<String, Double> scores,
    Instant analyzedAt
) {}