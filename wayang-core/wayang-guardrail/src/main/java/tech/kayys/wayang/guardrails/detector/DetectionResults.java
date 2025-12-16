package tech.kayys.wayang.guardrails.detector;

import java.util.List;
import java.util.stream.Collectors;

record DetectionResults(List<DetectionResult> results) {

    public boolean hasBlockingIssues() {
        return results.stream()
                .anyMatch(r -> r.severity() == DetectionSeverity.BLOCK);
    }

    public boolean hasRedactableContent() {
        return results.stream()
                .anyMatch(r -> !r.findings().isEmpty());
    }

    public String summary() {
        return results.stream()
                .filter(r -> r.severity() == DetectionSeverity.BLOCK)
                .map(DetectionResult::message)
                .collect(Collectors.joining("; "));
    }

    public List<String> detectorIds() {
        return results.stream()
                .filter(r -> r.severity() == DetectionSeverity.BLOCK)
                .map(DetectionResult::detectorId)
                .toList();
    }

    public List<?> getResults() {
        return results;
    }

    public boolean hasIssues() {
        return results.stream()
                .anyMatch(result -> {
                    if (result instanceof PIIDetector.PIIResult pii)
                        return pii.isDetected();
                    if (result instanceof ToxicityDetector.ToxicityResult tox)
                        return tox.isToxic();
                    if (result instanceof BiasDetector.BiasResult bias)
                        return bias.isBiased();
                    if (result instanceof HallucinationDetector.HallucinationResult hall)
                        return hall.isHallucinated();
                    return false;
                });
    }
}
