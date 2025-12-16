package tech.kayys.wayang.guardrails.policy;

record PolicyEvaluationResult(
        boolean allowed,
        String reason,
        String policyId) {
    static PolicyEvaluationResult allowed() {
        return new PolicyEvaluationResult(true, null, null);
    }

    static PolicyEvaluationResult denied(String reason, String policyId) {
        return new PolicyEvaluationResult(false, reason, policyId);
    }
}