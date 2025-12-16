package tech.kayys.wayang.guardrails;

import tech.kayys.wayang.guardrails.detector.*;
import tech.kayys.wayang.guardrails.policy.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class GuardrailsService {

    private static final Logger LOG = Logger.getLogger(GuardrailsService.class);

    @Inject
    PolicyEngine policyEngine;

    @Inject
    DetectorOrchestrator detectorOrchestrator;

    @Inject
    ContentRedactor contentRedactor;

    @Inject
    AuditService auditService;

    /**
     * Pre-execution guardrails check
     */
    public Uni<GuardrailResult> preCheck(NodeContext context) {
        LOG.debugf("Pre-check guardrails for node: %s", context.nodeId());

        return policyEngine.evaluatePolicies(context, CheckPhase.PRE_EXECUTION)
                .flatMap(policyResult -> {
                    if (!policyResult.allowed()) {
                        return Uni.createFrom().item(GuardrailResult.denied(
                                policyResult.reason(),
                                policyResult.policyId()));
                    }

                    // Run detectors on inputs
                    return detectorOrchestrator.detectInputIssues(context)
                            .map(detectionResults -> {
                                if (detectionResults.hasBlockingIssues()) {
                                    return GuardrailResult.denied(
                                            detectionResults.summary(),
                                            detectionResults.detectorIds());
                                }

                                return GuardrailResult.allowed();
                            });
                })
                .invoke(result -> auditGuardrailCheck(context, result, CheckPhase.PRE_EXECUTION));
    }

    /**
     * Post-execution guardrails check
     */
    public Uni<GuardrailResult> postCheck(ExecutionResult result) {
        LOG.debugf("Post-check guardrails for execution result");

        if (result.status() != ExecutionResult.Status.SUCCESS) {
            return Uni.createFrom().item(GuardrailResult.allowed()); // Skip check for errors
        }

        return detectorOrchestrator.detectOutputIssues(result)
                .flatMap(detectionResults -> {
                    if (detectionResults.hasBlockingIssues()) {
                        return Uni.createFrom().item(GuardrailResult.denied(
                                detectionResults.summary(),
                                detectionResults.detectorIds()));
                    }

                    // Apply redactions if needed
                    if (detectionResults.hasRedactableContent()) {
                        return contentRedactor.redact(result, detectionResults)
                                .map(redactedResult -> GuardrailResult.allowed()
                                        .withRedactedContent(redactedResult));
                    }

                    return Uni.createFrom().item(GuardrailResult.allowed());
                });
    }

    private void auditGuardrailCheck(
            NodeContext context,
            GuardrailResult result,
            CheckPhase phase) {
        auditService.audit(AuditPayload.builder()
                .event("GUARDRAIL_CHECK")
                .runId(context.runId())
                .nodeId(context.nodeId())
                .level(result.allowed() ? AuditLevel.INFO : AuditLevel.WARN)
                .actor(AuditPayload.Actor.system())
                .metadata(Map.of(
                        "phase", phase,
                        "allowed", result.allowed(),
                        "reason", result.reason()))
                .build());
    }
}