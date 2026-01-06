package tech.kayys.wayang.guardrails.dto;

public record BiasPolicy(
        double threshold,
        GuardrailAction action) {
}
