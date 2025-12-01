
@ApplicationScoped
public class PolicyBasedGuardrailsEngine implements GuardrailsEngine {
    @Inject PolicyEngine policyEngine;
    @Inject PIIDetector piiDetector;
    @Inject ContentModerator contentModerator;
    @Inject Redactor redactor;
    
    @Override
    public GuardrailResult preCheck(ExecuteNodeTask task) {
        List<PolicyViolation> violations = new ArrayList<>();
        
        // Check policies
        PolicyResult policyResult = policyEngine.evaluate(
            task.getNodeDescriptor().getCapabilities(),
            task.getMetadata().getTenantId()
        );
        violations.addAll(policyResult.getViolations());
        
        // PII detection
        PIIResult piiResult = piiDetector.detect(task.getInputPayload());
        if (piiResult.hasPII() && !task.getNodeDescriptor().getCapabilities().contains(Capability.PII_HANDLING)) {
            violations.add(new PolicyViolation(
                "PII_NOT_ALLOWED",
                "Node cannot handle PII data"
            ));
        }
        
        return GuardrailResult.builder()
            .allowed(violations.isEmpty())
            .violations(violations)
            .build();
    }
    
    @Override
    public GuardrailResult postCheck(ExecuteNodeTask task, ExecutionResult result) {
        List<PolicyViolation> violations = new ArrayList<>();
        
        // Content moderation
        ModerationResult modResult = contentModerator.moderate(
            result.getOutputs()
        );
        violations.addAll(modResult.getViolations());
        
        // PII redaction if needed
        if (modResult.hasPII()) {
            Map<String, Object> redacted = redactor.redact(result.getOutputs());
            result = result.withOutputs(redacted);
        }
        
        return GuardrailResult.builder()
            .allowed(violations.isEmpty())
            .violations(violations)
            .modifiedResult(result)
            .build();
    }
}