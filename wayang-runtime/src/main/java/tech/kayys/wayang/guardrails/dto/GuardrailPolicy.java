package tech.kayys.wayang.guardrails.dto;

import java.util.List;

public // Policy definitions
record GuardrailPolicy(
        boolean contentModerationEnabled,
        ContentModerationPolicy contentPolicy,
        boolean piiDetectionEnabled,
        PIIPolicy piiPolicy,
        boolean toxicityDetectionEnabled,
        ToxicityPolicy toxicityPolicy,
        boolean biasDetectionEnabled,
        BiasPolicy biasPolicy,
        boolean promptInjectionEnabled,
        boolean rateLimitEnabled,
        RateLimitConfig rateLimit,
        boolean costControlEnabled,
        boolean outputValidationEnabled,
        List<ValidationRule> validationRules) {
}