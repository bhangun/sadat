package tech.kayys.wayang.orchestration.strategy;

import io.quarkus.ai.agent.runtime.orchestration.*;
import io.quarkus.ai.agent.runtime.model.*;
import io.quarkus.ai.agent.runtime.context.ExecutionContext;
import io.quarkus.ai.agent.runtime.engine.WorkflowRuntimeEngine;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class SingleAgentStrategy implements OrchestrationStrategy {

    private static final Logger LOG = Logger.getLogger(SingleAgentStrategy.class);

    @Inject
    WorkflowRuntimeEngine workflowEngine;

    @Override
    public Uni<StrategyResult> execute(AgentDefinition agent, Map<String, Object> input,
            OrchestrationPattern pattern, ExecutionContext context) {

        LOG.info("Executing Single Agent pattern");

        List<OrchestrationEngine.OrchestrationStep> steps = Collections.synchronizedList(new ArrayList<>());

        // Get first workflow
        if (agent.getWorkflows() == null || agent.getWorkflows().isEmpty()) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Agent has no workflows"));
        }

        Workflow workflow = agent.getWorkflows().get(0);
        long startTime = System.currentTimeMillis();

        return workflowEngine.executeWorkflow(workflow, input, context)
                .map(result -> {
                    long duration = System.currentTimeMillis() - startTime;

                    steps.add(new OrchestrationEngine.OrchestrationStep(
                            agent.getId(),
                            agent.getName(),
                            System.currentTimeMillis(),
                            input,
                            result.getOutput(),
                            result.isSuccess() ? "completed" : "failed",
                            duration));

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("pattern", "Single Agent");
                    metadata.put("duration", duration);

                    return new StrategyResult(
                            result.isSuccess(),
                            result.getOutput(),
                            steps,
                            metadata);
                });
    }

    @Override
    public OrchestrationPattern.PatternType getSupportedPattern() {
        return OrchestrationPattern.PatternType.SINGLE_AGENT;
    }
}
