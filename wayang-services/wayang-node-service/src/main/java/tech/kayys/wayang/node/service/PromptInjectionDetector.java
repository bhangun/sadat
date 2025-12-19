package tech.kayys.wayang.workflow.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Prompt injection detector.
 */
@ApplicationScoped
class PromptInjectionDetector {

    private static final Logger LOG = Logger.getLogger(PromptInjectionDetector.class);

    // Patterns indicating prompt injection
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(previous|all|above)\\s+(instructions|prompts?)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(previous|all|above)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+(now|a)\\s+[a-z]+",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[SYSTEM\\]|<\\|system\\|>|###\\s*System",
                    Pattern.CASE_INSENSITIVE));

    public Uni<InjectionResult> detect(Map<String, Object> data) {
        return Uni.createFrom().item(() -> {
            String text = extractText(data);

            for (Pattern pattern : INJECTION_PATTERNS) {
                if (pattern.matcher(text).find()) {
                    return InjectionResult.builder()
                            .injectionDetected(true)
                            .reason("Pattern matched: " + pattern.pattern())
                            .build();
                }
            }

            return InjectionResult.builder()
                    .injectionDetected(false)
                    .build();
        });
    }

    private String extractText(Map<String, Object> data) {
        StringBuilder text = new StringBuilder();
        for (Object value : data.values()) {
            if (value instanceof String str) {
                text.append(str).append(" ");
            }
        }
        return text.toString();
    }
}
