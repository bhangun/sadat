package tech.kayys.wayang.guardrails.dto;

import java.util.Set;

public record PIIPolicy(
        Set<String> detectTypes,
        GuardrailAction action) {
}
