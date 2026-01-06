package tech.kayys.wayang.guardrails.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.guardrails.dto.GuardrailAction;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailPolicy;
import tech.kayys.wayang.guardrails.dto.GuardrailResult;
import tech.kayys.wayang.guardrails.dto.GuardrailViolation;
import tech.kayys.wayang.guardrails.dto.SanitizedOutput;

/**
 * Main guardrail engine coordinator
 */
@ApplicationScoped
public class GuardrailEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GuardrailEngine.class);

    @Inject
    ContentModerationService contentModeration;

    @Inject
    PIIDetectionService piiDetection;

    @Inject
    ToxicityDetectionService toxicityDetection;

    @Inject
    BiasDetectionService biasDetection;

    @Inject
    PromptInjectionDetector promptInjection;

    @Inject
    RateLimitService rateLimit;

    @Inject
    CostControlService costControl;

    @Inject
    OutputValidationService outputValidation;

    /**
     * Check input against all guardrails
     */
    public Uni<GuardrailResult> checkInput(
            String input,
            GuardrailPolicy policy,
            String userId,
            String tenantId) {

        LOG.debug("Checking input against guardrails for user: {}", userId);

        List<Uni<GuardrailCheckResult>> checks = new ArrayList<>();

        // Content moderation
        if (policy.contentModerationEnabled()) {
            checks.add(contentModeration.check(input, policy.contentPolicy()));
        }

        // PII detection
        if (policy.piiDetectionEnabled()) {
            checks.add(piiDetection.check(input, policy.piiPolicy()));
        }

        // Toxicity detection
        if (policy.toxicityDetectionEnabled()) {
            checks.add(toxicityDetection.check(input, policy.toxicityPolicy()));
        }

        // Prompt injection detection
        if (policy.promptInjectionEnabled()) {
            checks.add(promptInjection.check(input));
        }

        // Rate limiting
        if (policy.rateLimitEnabled()) {
            checks.add(rateLimit.check(userId, tenantId, policy.rateLimit()));
        }

        // Cost control
        if (policy.costControlEnabled()) {
            checks.add(costControl.checkTokenBudget(
                    userId, tenantId, estimateTokens(input)));
        }

        return Uni.join().all(checks).andCollectFailures()
                .map(results -> aggregateResults(results, "input"));
    }

    /**
     * Check output against guardrails
     */
    public Uni<GuardrailResult> checkOutput(
            String output,
            GuardrailPolicy policy,
            String userId,
            String tenantId) {

        LOG.debug("Checking output against guardrails");

        List<Uni<GuardrailCheckResult>> checks = new ArrayList<>();

        // Content moderation
        if (policy.contentModerationEnabled()) {
            checks.add(contentModeration.check(output, policy.contentPolicy()));
        }

        // PII detection and redaction
        if (policy.piiDetectionEnabled()) {
            checks.add(piiDetection.checkAndRedact(output, policy.piiPolicy()));
        }

        // Toxicity detection
        if (policy.toxicityDetectionEnabled()) {
            checks.add(toxicityDetection.check(output, policy.toxicityPolicy()));
        }

        // Bias detection
        if (policy.biasDetectionEnabled()) {
            checks.add(biasDetection.check(output, policy.biasPolicy()));
        }

        // Output validation
        if (policy.outputValidationEnabled()) {
            checks.add(outputValidation.validate(output, policy.validationRules()));
        }

        return Uni.join().all(checks).andCollectFailures()
                .map(results -> aggregateResults(results, "output"));
    }

    /**
     * Process output with sanitization
     */
    public Uni<SanitizedOutput> sanitizeOutput(
            String output,
            GuardrailPolicy policy) {

        String sanitized = output;
        List<String> modifications = new ArrayList<>();

        // PII redaction
        if (policy.piiDetectionEnabled() &&
                policy.piiPolicy().action() == GuardrailAction.REDACT) {
            sanitized = piiDetection.redact(sanitized);
            modifications.add("pii_redacted");
        }

        // Content filtering
        if (policy.contentModerationEnabled() &&
                policy.contentPolicy().action() == GuardrailAction.FILTER) {
            sanitized = contentModeration.filter(sanitized);
            modifications.add("content_filtered");
        }

        return Uni.createFrom().item(
                new SanitizedOutput(sanitized, modifications));
    }

    private GuardrailResult aggregateResults(
            List<GuardrailCheckResult> results,
            String stage) {

        boolean passed = results.stream().allMatch(GuardrailCheckResult::passed);

        List<GuardrailViolation> violations = results.stream()
                .filter(r -> !r.passed())
                .map(r -> new GuardrailViolation(
                        r.checkType(),
                        r.severity(),
                        r.message(),
                        r.details()))
                .toList();

        GuardrailAction action = determineAction(results);

        return new GuardrailResult(
                passed,
                action,
                violations,
                stage,
                Instant.now());
    }

    private GuardrailAction determineAction(List<GuardrailCheckResult> results) {
        // If any check requires blocking, block
        if (results.stream().anyMatch(r -> r.action() == GuardrailAction.BLOCK)) {
            return GuardrailAction.BLOCK;
        }

        // If any requires redaction, redact
        if (results.stream().anyMatch(r -> r.action() == GuardrailAction.REDACT)) {
            return GuardrailAction.REDACT;
        }

        // If any requires warning, warn
        if (results.stream().anyMatch(r -> r.action() == GuardrailAction.WARN)) {
            return GuardrailAction.WARN;
        }

        return GuardrailAction.ALLOW;
    }

    private int estimateTokens(String text) {
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }
}
