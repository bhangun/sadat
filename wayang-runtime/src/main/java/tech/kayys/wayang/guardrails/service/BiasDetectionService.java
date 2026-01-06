package tech.kayys.wayang.guardrails.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.dto.BiasPolicy;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailSeverity;

/**
 * Bias detection service
 */
@ApplicationScoped
public class BiasDetectionService {

    private static final Logger LOG = LoggerFactory.getLogger(BiasDetectionService.class);

    public Uni<GuardrailCheckResult> check(
            String content,
            BiasPolicy policy) {
        LOG.debug("Checking bias for content: {}", content);
        return Uni.createFrom().deferred(() -> {
            Map<String, Double> biasScores = analyzeBias(content);

            for (Map.Entry<String, Double> entry : biasScores.entrySet()) {
                if (entry.getValue() >= policy.threshold()) {
                    return Uni.createFrom().item(GuardrailCheckResult.violation(
                            "bias_detection",
                            GuardrailSeverity.LOW,
                            policy.action(),
                            String.format("Potential %s bias detected", entry.getKey()),
                            Map.of("bias_type", entry.getKey(),
                                    "score", entry.getValue())));
                }
            }

            return Uni.createFrom().item(GuardrailCheckResult.passed("bias_detection"));
        });
    }

    private Map<String, Double> analyzeBias(String content) {
        // Simplified bias detection
        // In production, use sophisticated ML models
        Map<String, Double> scores = new HashMap<>();
        scores.put("gender", 0.0);
        scores.put("racial", 0.0);
        scores.put("age", 0.0);
        return scores;
    }
}
