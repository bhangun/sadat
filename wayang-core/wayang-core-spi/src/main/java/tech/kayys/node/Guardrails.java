package tech.kayys.node;

import io.smallrye.mutiny.Uni;
import tech.kayys.execution.ExecutionResult;

public interface Guardrails {
    Uni<GuardrailResult> preCheck(NodeContext context, NodeDescriptor descriptor);
    Uni<GuardrailResult> postCheck(ExecutionResult result, NodeDescriptor descriptor);
}