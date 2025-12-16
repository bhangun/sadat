package tech.kayys.wayang.plugin.error;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.policy.PolicyEvaluationResult;

/**
 * Error Policy Engine
 */
class ErrorPolicyEngine {
    public Uni<PolicyEvaluationResult> evaluatePolicies(ErrorContext context) {
        return Uni.createFrom().item(
            PolicyEvaluationResult.builder().build()
        );
    }
}
