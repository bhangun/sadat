@Value
@Builder
public class PolicyViolation {
    String code;
    String message;
    Severity severity;
    String policyId;
}
