package tech.kayys.wayang.guardrails.service;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailSeverity;
import tech.kayys.wayang.guardrails.dto.ToxicityPolicy;

/**
 * Toxicity detection service
 */
@ApplicationScoped
public class ToxicityDetectionService {

    private static final Logger LOG = LoggerFactory.getLogger(ToxicityDetectionService.class);

    // Toxic keywords
    private static final Set<String> TOXIC_KEYWORDS = Set.of(
            "offensive", "toxic", "abusive", "threatening", "insulting");

    public Uni<GuardrailCheckResult> check(
            String content,
            ToxicityPolicy policy) {
        LOG.debug("Checking toxicity for content: {}", content);
        return Uni.createFrom().deferred(() -> {
            double toxicityScore = calculateToxicity(content);

            if (toxicityScore >= policy.threshold()) {
                return Uni.createFrom().item(GuardrailCheckResult.violation(
                        "toxicity_detection",
                        GuardrailSeverity.MEDIUM,
                        policy.action(),
                        String.format("High toxicity score: %.2f", toxicityScore),
                        Map.of("score", toxicityScore, "threshold", policy.threshold())));
            }

            return Uni.createFrom().item(GuardrailCheckResult.passed("toxicity_detection"));
        });
    }

    private double calculateToxicity(String content) {
        // Simple keyword-based scoring (use ML model in production)
        String lower = content.toLowerCase();

        long toxicCount = TOXIC_KEYWORDS.stream()
                .filter(lower::contains)
                .count();

        return Math.min(1.0, toxicCount * 0.2);
    }
}
