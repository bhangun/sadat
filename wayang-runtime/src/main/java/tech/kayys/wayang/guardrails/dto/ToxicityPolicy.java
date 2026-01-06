package tech.kayys.wayang.guardrails.dto;

public record ToxicityPolicy(
        double threshold,
        GuardrailAction action) {
}
