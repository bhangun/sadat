package tech.kayys.wayang.guardrails.detector;

import java.util.List;

record DetectionResult(
        String detectorId,
        DetectionSeverity severity,
        String message,
        List<Finding> findings) {
    static DetectionResult safe(String detectorId) {
        return new DetectionResult(detectorId, DetectionSeverity.INFO, "No issues", List.of());
    }

    static DetectionResult warning(String detectorId, String message, List<Finding> findings) {
        return new DetectionResult(detectorId, DetectionSeverity.WARN, message, findings);
    }

    static DetectionResult blocked(String detectorId, String message) {
        return new DetectionResult(detectorId, DetectionSeverity.BLOCK, message, List.of());
    }
}