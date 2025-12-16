package tech.kayys.wayang.guardrails.policy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;

import java.util.*;

public class PolicyEngine {

    @Inject
    PolicyRepository policyRepository;

    private final CelCompiler celCompiler;
    private final CelRuntime celRuntime;

    public PolicyEngine() {
        this.celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
                .addVar("input", SimpleType.DYN)
                .addVar("context", SimpleType.DYN)
                .addVar("tenant", SimpleType.STRING)
                .addVar("user", SimpleType.STRING)
                .build();

        this.celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
    }

    public Uni<PolicyEvaluationResult> evaluatePolicies(
            NodeContext context,
            CheckPhase phase) {
        return policyRepository.findActivePolices(context.tenantId(), phase)
                .flatMap(policies -> {
                    List<Uni<PolicyCheckResult>> evaluations = policies.stream()
                            .map(policy -> evaluatePolicy(policy, context))
                            .toList();

                    return Uni.join().all(evaluations).andFailFast()
                            .map(results -> aggregateResults(results));
                });
    }

    private Uni<PolicyCheckResult> evaluatePolicy(Policy policy, NodeContext context) {
        try {
            CelAbstractSyntaxTree ast = celCompiler.compile(policy.expression()).getAst();

            Map<String, Object> celContext = Map.of(
                    "input", context.inputs(),
                    "context", context,
                    "tenant", context.tenantId(),
                    "user", context.metadata().userId());

            Object result = celRuntime.createProgram(ast).eval(celContext);

            boolean allowed = Boolean.TRUE.equals(result);

            return Uni.createFrom().item(new PolicyCheckResult(
                    policy.id(),
                    policy.name(),
                    allowed,
                    allowed ? null : policy.denyMessage()));

        } catch (CelValidationException e) {
            return Uni.createFrom().failure(
                    new PolicyEvaluationException("Invalid CEL expression in policy: " + policy.id(), e));
        }
    }

    private PolicyEvaluationResult aggregateResults(List<PolicyCheckResult> results) {
        List<PolicyCheckResult> violations = results.stream()
                .filter(r -> !r.allowed())
                .toList();

        if (violations.isEmpty()) {
            return PolicyEvaluationResult.allowed();
        }

        return PolicyEvaluationResult.denied(
                violations.get(0).denyMessage(),
                violations.get(0).policyId());
    }
}