package tech.kayys.wayang.dto;

import java.util.List;

import tech.kayys.wayang.guardrails.dto.GuardrailViolation;

public record GuardedAgentResponse(
        String taskId,
        boolean success,
        String response,
        List<GuardrailViolation> violations,
        List<String> modifications) {
}