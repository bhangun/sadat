package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.execution.ExecutionResult;

import java.util.*;

@ApplicationScoped
public class ContentRedactor {

    public Uni<Map<String, Object>> redact(
            ExecutionResult result,
            DetectionResults detections) {
        Map<String, Object> redacted = new HashMap<>(result.outputs());

        for (Map.Entry<String, Object> entry : redacted.entrySet()) {
            if (entry.getValue() instanceof String text) {
                String redactedText = redactText(text, detections);
                redacted.put(entry.getKey(), redactedText);
            }
        }

        return Uni.createFrom().item(redacted);
    }

    private String redactText(String text, DetectionResults detections) {
        String redacted = text;

        // Sort findings by offset (descending) to maintain string positions
        List<Finding> sortedFindings = detections.results().stream()
                .flatMap(r -> r.findings().stream())
                .sorted(Comparator.comparing(Finding::startOffset).reversed())
                .toList();

        for (Finding finding : sortedFindings) {
            String replacement = switch (finding.type()) {
                case "SSN" -> "[SSN_REDACTED]";
                case "CREDIT_CARD" -> "[CARD_REDACTED]";
                case "EMAIL" -> "[EMAIL_REDACTED]";
                case "PHONE" -> "[PHONE_REDACTED]";
                default -> "[REDACTED]";
            };

            redacted = redacted.substring(0, finding.startOffset()) +
                    replacement +
                    redacted.substring(finding.endOffset());
        }

        return redacted;
    }
}