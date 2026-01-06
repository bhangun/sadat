package tech.kayys.wayang.dto;

import tech.kayys.wayang.guardrails.dto.GuardrailPolicy;

public record GuardrailTestRequest(
        String content,
        GuardrailPolicy policy) {
}
