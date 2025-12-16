package tech.kayys.wayang.guardrails.policy;

import tech.kayys.wayang.guardrails.Severity;

public class PolicyViolation {
    String code;
    String message;
    Severity severity;
    String policyId;
}
