package tech.kayys.wayang.guardrails.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.dto.ContentModerationPolicy;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailSeverity;

/**
 * Content moderation service
 */
@ApplicationScoped
public class ContentModerationService {

    private static final Logger LOG = LoggerFactory.getLogger(ContentModerationService.class);

    // Blocked categories
    private static final Set<String> BLOCKED_CATEGORIES = Set.of(
            "violence", "hate_speech", "sexual_content", "self_harm",
            "illegal_activity", "dangerous_content");

    // Keyword patterns
    private final Map<String, List<Pattern>> categoryPatterns;

    public ContentModerationService() {
        this.categoryPatterns = initializeCategoryPatterns();
    }

    public Uni<GuardrailCheckResult> check(
            String content,
            ContentModerationPolicy policy) {
        LOG.debug("Checking content moderation for content: {}", content);
        return Uni.createFrom().deferred(() -> {
            Map<String, Double> scores = analyzeContent(content);

            // Check thresholds
            for (Map.Entry<String, Double> entry : scores.entrySet()) {
                String category = entry.getKey();
                double score = entry.getValue();
                double threshold = policy.thresholds().getOrDefault(
                        category, 0.8);

                if (score >= threshold) {
                    return Uni.createFrom().item(GuardrailCheckResult.violation(
                            "content_moderation",
                            GuardrailSeverity.HIGH,
                            policy.action(),
                            String.format("Content flagged for %s (score: %.2f)",
                                    category, score),
                            Map.of("category", category, "score", score)));
                }
            }

            return Uni.createFrom().item(GuardrailCheckResult.passed("content_moderation"));
        });
    }

    public String filter(String content) {
        // Filter inappropriate content
        String filtered = content;

        for (Map.Entry<String, List<Pattern>> entry : categoryPatterns.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                filtered = pattern.matcher(filtered)
                        .replaceAll("[FILTERED]");
            }
        }

        return filtered;
    }

    private Map<String, Double> analyzeContent(String content) {
        Map<String, Double> scores = new HashMap<>();

        String lowerContent = content.toLowerCase();

        // Simple pattern matching (in production, use ML model)
        for (Map.Entry<String, List<Pattern>> entry : categoryPatterns.entrySet()) {
            String category = entry.getKey();
            int matches = 0;

            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(lowerContent).find()) {
                    matches++;
                }
            }

            double score = Math.min(1.0, matches * 0.3);
            scores.put(category, score);
        }

        return scores;
    }

    private Map<String, List<Pattern>> initializeCategoryPatterns() {
        Map<String, List<Pattern>> patterns = new HashMap<>();

        // Violence patterns
        patterns.put("violence", List.of(
                Pattern.compile("\\b(kill|murder|attack|assault|weapon)\\b",
                        Pattern.CASE_INSENSITIVE)));

        // Hate speech patterns
        patterns.put("hate_speech", List.of(
                Pattern.compile("\\b(racist|bigot|discrimination)\\b",
                        Pattern.CASE_INSENSITIVE)));

        // Add more categories...

        return patterns;
    }
}
