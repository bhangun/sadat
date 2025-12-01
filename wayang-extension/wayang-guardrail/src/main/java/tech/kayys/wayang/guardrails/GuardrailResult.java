@Value
@Builder
public class GuardrailResult {
    boolean allowed;
    List<PolicyViolation> violations;
    Optional<ExecutionResult> modifiedResult;
    Map<String, Object> metadata;
}
