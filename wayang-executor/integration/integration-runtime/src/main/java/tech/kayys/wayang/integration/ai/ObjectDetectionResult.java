package tech.kayys.silat.executor.camel.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record ObjectDetectionResult(
    List<Map<String, Object>> detections,
    int objectCount,
    Instant detectedAt
) {}