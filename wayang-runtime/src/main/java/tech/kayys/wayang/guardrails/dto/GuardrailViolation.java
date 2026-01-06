package tech.kayys.wayang.guardrails.dto;

import java.util.Map;

public record GuardrailViolation(
        String checkType,
        GuardrailSeverity severity,
        String message,
        Map<String, Object> details) {
}