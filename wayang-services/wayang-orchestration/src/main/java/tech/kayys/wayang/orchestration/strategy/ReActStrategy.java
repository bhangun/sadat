package tech.kayys.wayang.orchestration.strategy;

/**
 * ReAct (Reasoning + Acting) Pattern
 */

import io.quarkus.ai.agent.runtime.orchestration.*;
import io.quarkus.ai.agent.runtime.model.*;
import io.quarkus.ai.agent.runtime.context.ExecutionContext;
import io.quarkus.ai.agent.runtime.service.LLMService;
import io.quarkus.ai.agent.runtime.service.ToolService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class ReActStrategy implements OrchestrationStrategy {

    private static final Logger LOG = Logger.getLogger(ReActStrategy.class);
    private static final int MAX_ITERATIONS = 10;

    @Inject
    LLMService llmService;

    @Inject
    ToolService toolService;

    @Override
    public Uni<StrategyResult> execute(AgentDefinition agent, Map<String, Object> input,
            OrchestrationPattern pattern, ExecutionContext context) {

        LOG.info("Executing ReAct pattern");

        List<OrchestrationEngine.OrchestrationStep> steps = Collections.synchronizedList(new ArrayList<>());
        String task = (String) input.get("task");

        return reactLoop(agent, task, context, steps, 0)
                .map(finalAnswer -> {
                    Map<String, Object> output = new HashMap<>();
                    output.put("answer", finalAnswer);
                    output.put("iterations", steps.size());

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("pattern", "ReAct");
                    metadata.put("totalSteps", steps.size());

                    return new StrategyResult(true, output, steps, metadata);
                });
    }

    private Uni<String> reactLoop(AgentDefinition agent, String task, ExecutionContext context,
            List<OrchestrationEngine.OrchestrationStep> steps, int iteration) {

        if (iteration >= MAX_ITERATIONS) {
            return Uni.createFrom().item("Maximum iterations reached without solution");
        }

        // Build prompt with previous steps
        String prompt = buildReActPrompt(task, steps);

        long startTime = System.currentTimeMillis();

        // Reasoning step: Ask LLM what to do next
        return llmService.complete(agent.getLlmConfig(), prompt, context)
                .chain(response -> {
                    long duration = System.currentTimeMillis() - startTime;

                    // Parse response to extract thought, action, and action input
                    ReActResponse parsed = parseReActResponse(response);

                    // Add reasoning step
                    steps.add(new OrchestrationEngine.OrchestrationStep(
                            agent.getId(),
                            agent.getName() + " (Thought)",
                            System.currentTimeMillis(),
                            Map.of("prompt", prompt),
                            Map.of("thought", parsed.thought),
                            "completed",
                            duration));

                    // Check if we have final answer
                    if (parsed.isFinalAnswer) {
                        return Uni.createFrom().item(parsed.answer);
                    }

                    // Acting step: Execute the tool
                    Tool tool = findTool(agent, parsed.action);
                    if (tool == null) {
                        LOG.warnf("Tool not found: %s", parsed.action);
                        return reactLoop(agent, task, context, steps, iteration + 1);
                    }

                    long toolStartTime = System.currentTimeMillis();
                    return toolService.executeTool(tool, parsed.actionInput, context)
                            .chain(toolResult -> {
                                long toolDuration = System.currentTimeMillis() - toolStartTime;

                                // Add action step
                                steps.add(new OrchestrationEngine.OrchestrationStep(
                                        tool.getId(),
                                        tool.getName(),
                                        System.currentTimeMillis(),
                                        parsed.actionInput,
                                        toolResult,
                                        "completed",
                                        toolDuration));

                                // Store observation in context
                                context.setVariable("observation_" + iteration, toolResult);

                                // Continue loop with observation
                                return reactLoop(agent, task, context, steps, iteration + 1);
                            });
                })
                .onFailure().recoverWithUni(error -> {
                    LOG.errorf(error, "ReAct iteration failed");
                    return Uni.createFrom().item("Error: " + error.getMessage());
                });
    }

    private String buildReActPrompt(String task, List<OrchestrationEngine.OrchestrationStep> steps) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are solving this task: ").append(task).append("\n\n");
        prompt.append("You can use the following format:\n");
        prompt.append("Thought: <your reasoning about what to do next>\n");
        prompt.append("Action: <action to take>\n");
        prompt.append("Action Input: <input for the action>\n");
        prompt.append("Observation: <result of the action>\n");
        prompt.append("... (repeat Thought/Action/Observation as needed)\n");
        prompt.append("Thought: I now know the final answer\n");
        prompt.append("Final Answer: <the final answer to the task>\n\n");

        // Add previous steps
        if (!steps.isEmpty()) {
            prompt.append("Previous steps:\n");
            for (int i = 0; i < steps.size(); i++) {
                OrchestrationEngine.OrchestrationStep step = steps.get(i);
                if (step.getAgentName().contains("Thought")) {
                    prompt.append("Thought: ").append(step.getOutput().get("thought")).append("\n");
                } else {
                    prompt.append("Action: ").append(step.getAgentName()).append("\n");
                    prompt.append("Observation: ").append(step.getOutput()).append("\n");
                }
            }
        }

        prompt.append("\nWhat is your next step?");
        return prompt.toString();
    }

    private ReActResponse parseReActResponse(String response) {
        ReActResponse result = new ReActResponse();

        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.startsWith("Thought:")) {
                result.thought = line.substring("Thought:".length()).trim();
            } else if (line.startsWith("Action:")) {
                result.action = line.substring("Action:".length()).trim();
            } else if (line.startsWith("Action Input:")) {
                String input = line.substring("Action Input:".length()).trim();
                result.actionInput = Map.of("input", input);
            } else if (line.startsWith("Final Answer:")) {
                result.isFinalAnswer = true;
                result.answer = line.substring("Final Answer:".length()).trim();
            }
        }

        return result;
    }

    private Tool findTool(AgentDefinition agent, String toolName) {
        if (agent.getTools() == null)
            return null;

        return agent.getTools().stream()
                .filter(t -> t.getName().equalsIgnoreCase(toolName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public OrchestrationPattern.PatternType getSupportedPattern() {
        return OrchestrationPattern.PatternType.REACT;
    }

    private static class ReActResponse {
        String thought = "";
        String action = "";
        Map<String, Object> actionInput = new HashMap<>();
        boolean isFinalAnswer = false;
        String answer = "";
    }
}
