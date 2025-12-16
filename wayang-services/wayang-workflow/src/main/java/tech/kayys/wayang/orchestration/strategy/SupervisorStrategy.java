package tech.kayys.wayang.orchestration.strategy;

import io.quarkus.ai.agent.runtime.orchestration.*;
import io.quarkus.ai.agent.runtime.model.*;
import io.quarkus.ai.agent.runtime.context.ExecutionContext;
import io.quarkus.ai.agent.runtime.service.LLMService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class SupervisorStrategy implements OrchestrationStrategy {

    private static final Logger LOG = Logger.getLogger(SupervisorStrategy.class);

    @Inject
    LLMService llmService;

    @Override
    public Uni<StrategyResult> execute(AgentDefinition agent, Map<String, Object> input,
            OrchestrationPattern pattern, ExecutionContext context) {

        LOG.info("Executing Supervisor pattern");

        List<OrchestrationEngine.OrchestrationStep> steps = Collections.synchronizedList(new ArrayList<>());

        String task = (String) input.get("task");

        // Supervisor continuously monitors and adjusts worker execution
        return superviseExecution(agent, task, context, steps, 0)
                .map(result -> {
                    Map<String, Object> output = new HashMap<>();
                    output.put("result", result);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("pattern", "Supervisor");
                    metadata.put("totalSteps", steps.size());

                    return new StrategyResult(true, output, steps, metadata);
                });
    }

    private Uni<String> superviseExecution(AgentDefinition agent, String task,
            ExecutionContext context,
            List<OrchestrationEngine.OrchestrationStep> steps,
            int iteration) {

        if (iteration >= 5) {
            return Uni.createFrom().item("Maximum supervision iterations reached");
        }

        // Supervisor decides next action
        String supervisionPrompt = buildSupervisionPrompt(task, steps, iteration);

        return llmService.complete(agent.getLlmConfig(), supervisionPrompt, context)
                .chain(decision -> {
                    steps.add(new OrchestrationEngine.OrchestrationStep(
                            agent.getId(),
                            "Supervisor (Decision " + iteration + ")",
                            System.currentTimeMillis(),
                            Map.of("task", task),
                            Map.of("decision", decision),
                            "completed",
                            0));

                    // Parse decision
                    if (decision.toLowerCase().contains("complete") ||
                            decision.toLowerCase().contains("done")) {
                        return Uni.createFrom().item(decision);
                    }

                    // Continue supervision
                    return superviseExecution(agent, task, context, steps, iteration + 1);
                });
    }

    private String buildSupervisionPrompt(String task,
            List<OrchestrationEngine.OrchestrationStep> steps,
            int iteration) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a supervisor overseeing the completion of this task: ")
                .append(task).append("\n\n");

        if (!steps.isEmpty()) {
            prompt.append("Previous actions:\n");
            steps.forEach(step -> {
                prompt.append("- ").append(step.getAgentName()).append(": ")
                        .append(step.getOutput()).append("\n");
            });
        }

        prompt.append("\nAs the supervisor, what should be done next? ")
                .append("Respond with your decision or say 'COMPLETE' if the task is done.");

        return prompt.toString();
    }

    @Override
    public OrchestrationPattern.PatternType getSupportedPattern() {
        return OrchestrationPattern.PatternType.SUPERVISOR;
    }
}
