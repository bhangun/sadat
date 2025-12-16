package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.llm.service.LLMService;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class DecisionNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(DecisionNodeExecutor.class);

    @Inject
    LLMService llmService;

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        // Decision node uses LLM to make intelligent decisions

        if (node.getConfig() == null || node.getConfig().getPrompt() == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Decision node requires prompt configuration"));
        }

        String prompt = buildDecisionPrompt(node, context);

        return llmService.complete(
                node.getConfig().getLlmConfig(),
                prompt,
                context)
                .map(decision -> {
                    Map<String, Object> output = new HashMap<>();
                    output.put("decision", decision);
                    output.put("confidence", extractConfidence(decision));

                    return new NodeExecutionResult(node.getId(), true, output, null);
                });
    }

    private String buildDecisionPrompt(Workflow.Node node, ExecutionContext context) {
        String basePrompt = node.getConfig().getPrompt();

        // Add context variables
        StringBuilder prompt = new StringBuilder();
        prompt.append("Make a decision based on the following context:\n\n");

        context.getAllVariables().forEach((key, value) -> {
            prompt.append(key).append(": ").append(value).append("\n");
        });

        prompt.append("\nDecision required: ").append(basePrompt);
        prompt.append("\n\nRespond with your decision and confidence level (0-1).");

        return prompt.toString();
    }

    private double extractConfidence(String decision) {
        // Try to extract confidence from LLM response
        String lower = decision.toLowerCase();
        if (lower.contains("confidence: ")) {
            try {
                String conf = lower.split("confidence: ")[1].split("\\s")[0];
                return Double.parseDouble(conf.replaceAll("[^0-9.]", ""));
            } catch (Exception e) {
                return 0.8; // Default confidence
            }
        }
        return 0.8;
    }

    @Override
    public Workflow.Node.NodeType getSupportedType() {
        return Workflow.Node.NodeType.DECISION;
    }
}