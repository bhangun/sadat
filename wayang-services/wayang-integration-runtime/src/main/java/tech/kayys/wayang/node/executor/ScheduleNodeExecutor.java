package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class ScheduleNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(ScheduleNodeExecutor.class);

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        // Schedule node registers for future execution
        // In a full implementation, this would integrate with Quarkus Scheduler

        Map<String, Object> output = new HashMap<>();
        output.put("scheduledAt", Instant.now());
        output.put("status", "scheduled");

        // Store schedule info in context
        context.setVariable("nextScheduledRun", calculateNextRun(node));

        LOG.infof("Schedule node executed: %s", node.getName());

        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, output, null));
    }

    private Instant calculateNextRun(Workflow.Node node) {
        // Simple implementation - in production, use proper cron parser
        return Instant.now().plusSeconds(3600); // Default: 1 hour from now
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.SCHEDULE;
    }
}
