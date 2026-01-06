package tech.kayys.wayang.guardrails.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.dto.GuardrailAction;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailSeverity;

/**
 * Prompt injection attack detector
 */
@ApplicationScoped
public class PromptInjectionDetector {

    private static final Logger LOG = LoggerFactory.getLogger(PromptInjectionDetector.class);

    // Injection patterns
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (previous|above|all) (instructions|prompts)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("system prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdan\\b.*mode", Pattern.CASE_INSENSITIVE));

    public Uni<GuardrailCheckResult> check(String content) {
        LOG.debug("Checking prompt injection for content: {}", content);
        return Uni.createFrom().deferred(() -> {
            for (Pattern pattern : INJECTION_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    return Uni.createFrom().item(GuardrailCheckResult.violation(
                            "prompt_injection",
                            GuardrailSeverity.CRITICAL,
                            GuardrailAction.BLOCK,
                            "Potential prompt injection detected",
                            Map.of("pattern", pattern.pattern())));
                }
            }

            return Uni.createFrom().item(GuardrailCheckResult.passed("prompt_injection"));
        });
    }
}