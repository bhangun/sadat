package tech.kayys.wayang.plugin.guardrails;

/**
 * Guardrail result
 */
public class GuardrailResult {
    private final boolean allowed;
    private final String reason;
    
    private GuardrailResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }
    
    public static GuardrailResult allow() {
        return new GuardrailResult(true, null);
    }
    
    public static GuardrailResult deny(String reason) {
        return new GuardrailResult(false, reason);
    }
    
    public boolean isAllowed() { return allowed; }
    public String getReason() { return reason; }
}
