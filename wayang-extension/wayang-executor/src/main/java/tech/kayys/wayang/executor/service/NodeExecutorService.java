package tech.kayys.wayang.executor.service;

import tech.kayys.wayang.common.contract.Node;
import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.executor.sandbox.SandboxManager;
import tech.kayys.wayang.executor.plugin.PluginLoader;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NodeExecutorService {
    
    private static final Logger LOG = Logger.getLogger(NodeExecutorService.class);
    
    @Inject
    PluginLoader pluginLoader;
    
    @Inject
    SandboxManager sandboxManager;
    
    @Inject
    GuardrailsService guardrails;
    
    @Inject
    AuditService auditService;
    
    public Uni<ExecutionResult> executeNode(NodeContext context) {
        LOG.infof("Executing node: %s", context.nodeId());
        
        return pluginLoader.loadNode(context.nodeId())
            .flatMap(node -> sandboxManager.executeInSandbox(
                () -> executeWithGuardrails(node, context)
            ))
            .onFailure().recoverWithItem(failure -> 
                ExecutionResult.error(ErrorPayload.from(failure))
            )
            .invoke(result -> auditService.auditExecution(context, result));
    }
    
    private Uni<ExecutionResult> executeWithGuardrails(Node node, NodeContext context) {
        return guardrails.preCheck(context)
            .flatMap(preCheckResult -> {
                if (!preCheckResult.allowed()) {
                    return Uni.createFrom().item(
                        ExecutionResult.error(preCheckResult.error())
                    );
                }
                
                return node.execute(context)
                    .flatMap(result -> guardrails.postCheck(result)
                        .map(postCheckResult -> 
                            postCheckResult.allowed() ? result : 
                            ExecutionResult.error(postCheckResult.error())
                        )
                    );
            });
    }
}