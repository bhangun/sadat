package tech.kayys.wayang.guardrails.dto;

import java.time.Instant;
import java.util.List;

public record GuardrailResult(
        boolean passed,
        GuardrailAction action,
        List<GuardrailViolation> violations,
        String stage,
        Instant checkedAt) {
}
