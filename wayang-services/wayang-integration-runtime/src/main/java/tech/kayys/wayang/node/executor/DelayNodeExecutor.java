package tech.kayys.wayang.node.executor;

import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

/**
 * DELAY node executor
 */
@ApplicationScoped
public class DelayNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(DelayNodeExecutor.class);

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        // Get delay duration from node config
        Object delayConfig = node.getConfig() != null ? node.getConfig().getTransformConfig() : null;

        int delayMs = 1000; // default 1 second

        if (delayConfig != null && delayConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) delayConfig;
            if (config.containsKey("duration")) {
                delayMs = (int) config.get("duration");
            }
        }

        LOG.debugf("Delaying execution for %d ms", delayMs);

        return Uni.createFrom().voidItem()
                .onItem().delayIt().by(java.time.Duration.ofMillis(delayMs))
                .map(v -> new NodeExecutionResult(
                        node.getId(),
                        true,
                        Map.of("delayed", delayMs),
                        null));
    }

    @Override
    public Workflow.Node.NodeType getSupportedType() {
        return Workflow.Node.NodeType.DELAY;
    }
}