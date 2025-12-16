package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.llm.service.LLMService;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM node executor
 */
@ApplicationScoped
public class LLMNodeExecutor implements NodeExecutor {

    @Inject
    LLMService llmService;

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        LLMConfig llmConfig = node.getConfig().getLlmConfig();
        String prompt = buildPrompt(node, context);

        return llmService.complete(llmConfig, prompt, context)
                .map(response -> {
                    Map<String, Object> output = new HashMap<>();
                    output.put("response", response);
                    return new NodeExecutionResult(node.getId(), true, output, null);
                });
    }

    private String buildPrompt(Workflow.Node node, ExecutionContext context) {
        if (node.getConfig().getPromptTemplate() != null) {
            return context.interpolateTemplate(
                    node.getConfig().getPromptTemplate().getTemplate(),
                    node.getConfig().getPromptTemplate().getVariables());
        }
        return node.getConfig().getPrompt();
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.LLM;
    }
}
