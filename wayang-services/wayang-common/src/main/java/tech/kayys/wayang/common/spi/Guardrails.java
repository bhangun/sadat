package tech.kayys.wayang.common.spi;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.node.NodeDefinition;

public interface Guardrails {
    Uni<GuardrailResult> preCheck(NodeContext context, NodeDefinition descriptor);
    Uni<GuardrailResult> postCheck(ExecutionResult result, NodeDefinition descriptor);
}

class GuardrailResult {
    private final boolean allowed;
    private final String reason;

    private GuardrailResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    public boolean isAllowed() { return allowed; }
    public String getReason() { return reason; }

    public static GuardrailResult allow() {
        return new GuardrailResult(true, null);
    }
    
    public static GuardrailResult block(String reason) {
        return new GuardrailResult(false, reason);
    }
}
