package tech.kayys.wayang.guardrails.dto;

import java.util.Map;

public record ContentModerationPolicy(
        Map<String, Double> thresholds,
        GuardrailAction action) {
}
