package tech.kayys.wayang.guardrails.policy;

import tech.kayys.wayang.guardrails.detector.CheckPhase;

public record Policy(
        String id,
        String name,
        String expression,
        String denyMessage,
        PolicySeverity severity,
        CheckPhase phase) {
}