package tech.kayys.wayang.dto;

import java.time.Instant;

import tech.kayys.wayang.guardrails.dto.GuardrailResult;

public record GuardrailTestResponse(
        GuardrailResult inputResult,
        GuardrailResult outputResult,
        Instant testedAt) {
}