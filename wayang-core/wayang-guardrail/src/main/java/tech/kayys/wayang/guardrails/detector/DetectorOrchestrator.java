package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.execution.ExecutionResult;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DetectorOrchestrator {

    @Inject
    PIIDetector piiDetector;

    @Inject
    ToxicityDetector toxicityDetector;

    @Inject
    BiasDetector biasDetector;

    @Inject
    HallucinationDetector hallucinationDetector;

    public Uni<DetectionResults> detectInputIssues(NodeContext context) {
        String inputText = extractText(context.inputs());

        return Uni.combine().all().unis(
                piiDetector.detect(inputText),
                toxicityDetector.detect(inputText),
                biasDetector.detect(inputText))
                .combinedWith((pii, toxicity, bias) -> new DetectionResults(List.of(pii, toxicity, bias)));
    }

    public Uni<DetectionResults> detectOutputIssues(ExecutionResult result) {
        String outputText = extractText(result.outputs());

        return Uni.combine().all().unis(
                piiDetector.detect(outputText),
                toxicityDetector.detect(outputText),
                hallucinationDetector.detect(outputText, result.metadata())).combinedWith(
                        (pii, toxicity, hallucination) -> new DetectionResults(List.of(pii, toxicity, hallucination)));
    }

    private String extractText(Map<String, Object> data) {
        return data.values().stream()
                .filter(v -> v instanceof String)
                .map(Object::toString)
                .collect(Collectors.joining(" "));
    }
}