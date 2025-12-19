package tech.kayys.wayang.orchestration.strategy;

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

/**
 * Plan-Execute Pattern
 */

@ApplicationScoped
public class PlanExecuteStrategy implements OrchestrationStrategy {

    private static final Logger LOG = Logger.getLogger(PlanExecuteStrategy.class);

    @Inject
    LLMService llmService;

    @Inject
    ToolService toolService;

    @Override
    public Uni<StrategyResult> execute(AgentDefinition agent, Map<String, Object> input,
            OrchestrationPattern pattern, ExecutionContext context) {

        LOG.info("Executing Plan-Execute pattern");

        List<OrchestrationEngine.OrchestrationStep> steps = Collections.synchronizedList(new ArrayList<>());
        String task = (String) input.get("task");

        // Phase 1: Planning
        return planPhase(agent, task, context, steps)
                .chain(plan -> {
                    // Phase 2: Execution
                    return executePhase(agent, plan, context, steps);
                })
                .map(finalResult -> {
                    Map<String, Object> output = new HashMap<>();
                    output.put("result", finalResult);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("pattern", "Plan-Execute");
                    metadata.put("totalSteps", steps.size());

                    return new StrategyResult(true, output, steps, metadata);
                });
    }

    private Uni<List<PlanStep>> planPhase(AgentDefinition agent, String task,
            ExecutionContext context,
            List<OrchestrationEngine.OrchestrationStep> steps) {

        String planPrompt = buildPlanPrompt(task, agent.getTools());
        long startTime = System.currentTimeMillis();

        return llmService.complete(agent.getLlmConfig(), planPrompt, context)
                .map(planResponse -> {
                    long duration = System.currentTimeMillis() - startTime;

                    List<PlanStep> plan = parsePlan(planResponse);

                    steps.add(new OrchestrationEngine.OrchestrationStep(
                            agent.getId(),
                            agent.getName() + " (Planner)",
                            System.currentTimeMillis(),
                            Map.of("task", task),
                            Map.of("plan", plan),
                            "completed",
                            duration));

                    return plan;
                });
    }

    private Uni<String> executePhase(AgentDefinition agent, List<PlanStep> plan,
            ExecutionContext context,
            List<OrchestrationEngine.OrchestrationStep> steps) {

        return executePlanSteps(agent, plan, 0, context, steps, new HashMap<>());
    }

    private Uni<String> executePlanSteps(AgentDefinition agent, List<PlanStep> plan, int stepIndex,
            ExecutionContext context,
            List<OrchestrationEngine.OrchestrationStep> steps,
            Map<String, Object> results) {

        if (stepIndex >= plan.size()) {
            // All steps executed, synthesize final answer
            return synthesizeFinalAnswer(agent, plan, results, context, steps);
        }

        PlanStep currentStep = plan.get(stepIndex);
        long startTime = System.currentTimeMillis();

        // Execute current step
        Tool tool = findTool(agent, currentStep.tool);
        if (tool == null) {
            LOG.warnf("Tool not found: %s", currentStep.tool);
            return executePlanSteps(agent, plan, stepIndex + 1, context, steps, results);
        }

        return toolService.executeTool(tool, currentStep.input, context)
                .chain(toolResult -> {
                    long duration = System.currentTimeMillis() - startTime;

                    steps.add(new OrchestrationEngine.OrchestrationStep(
                            tool.getId(),
                            "Step " + (stepIndex + 1) + ": " + currentStep.description,
                            System.currentTimeMillis(),
                            currentStep.input,
                            toolResult,
                            "completed",
                            duration));

                    // Store result
                    results.put("step_" + stepIndex, toolResult);
                    context.setVariable("step_" + stepIndex + "_result", toolResult);

                    // Continue to next step
                    return executePlanSteps(agent, plan, stepIndex + 1, context, steps, results);
                });
    }

    private Uni<String> synthesizeFinalAnswer(AgentDefinition agent, List<PlanStep> plan,
            Map<String, Object> results, ExecutionContext context,
            List<OrchestrationEngine.OrchestrationStep> steps) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following plan execution results, provide a final answer:\n\n");

        for (int i = 0; i < plan.size(); i++) {
            prompt.append("Step ").append(i + 1).append(": ").append(plan.get(i).description).append("\n");
            prompt.append("Result: ").append(results.get("step_" + i)).append("\n\n");
        }

        prompt.append("Final Answer:");

        return llmService.complete(agent.getLlmConfig(), prompt.toString(), context);
    }

    private String buildPlanPrompt(String task, List<Tool> availableTools) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a step-by-step plan to accomplish this task: ").append(task).append("\n\n");
        prompt.append("Available tools:\n");

        if (availableTools != null) {
            for (Tool tool : availableTools) {
                prompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
            }
        }

        prompt.append("\nProvide a plan in this format:\n");
        prompt.append("Step 1: [description]\n");
        prompt.append("Tool: [tool name]\n");
        prompt.append("Input: [what input to provide]\n");
        prompt.append("---\n");
        prompt.append("(repeat for each step)\n\n");
        prompt.append("Create the plan now:");

        return prompt.toString();
    }

    private List<PlanStep> parsePlan(String planResponse) {
        List<PlanStep> plan = new ArrayList<>();
        String[] sections = planResponse.split("---");

        for (String section : sections) {
            String[] lines = section.trim().split("\n");
            if (lines.length < 3)
                continue;

            PlanStep step = new PlanStep();
            for (String line : lines) {
                if (line.startsWith("Step")) {
                    step.description = line.substring(line.indexOf(":") + 1).trim();
                } else if (line.startsWith("Tool:")) {
                    step.tool = line.substring("Tool:".length()).trim();
                } else if (line.startsWith("Input:")) {
                    String input = line.substring("Input:".length()).trim();
                    step.input = Map.of("input", input);
                }
            }

            if (!step.tool.isEmpty()) {
                plan.add(step);
            }
        }

        return plan;
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
        return OrchestrationPattern.PatternType.PLAN_EXECUTE;
    }

    private static class PlanStep {
        String description = "";
        String tool = "";
        Map<String, Object> input = new HashMap<>();
    }
}