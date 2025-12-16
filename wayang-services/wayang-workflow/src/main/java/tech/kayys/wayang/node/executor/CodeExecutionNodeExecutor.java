package tech.kayys.wayang.node.executor;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

/**
 * CODE_EXECUTION node executor
 */
@ApplicationScoped
public class CodeExecutionNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(CodeExecutionNodeExecutor.class);

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        Workflow.Node.NodeConfig.TransformConfig config = node.getConfig().getTransformConfig();

        if (config == null || config.getScript() == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Code execution requires script configuration"));
        }

        return Uni.createFrom().item(() -> {
            String language = config.getType() != null ? config.getType() : "javascript";
            Map<String, Object> input = context.getAllVariables();

            Map<String, Object> result = executeCode(language, config.getScript(), input);

            return new NodeExecutionResult(node.getId(), true, result, null);
        });
    }

    private Map<String, Object> executeCode(String language, String script, Map<String, Object> input) {
        try {
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine;

            switch (language.toLowerCase()) {
                case "javascript", "js":
                    engine = manager.getEngineByName("javascript");
                    break;
                case "python":
                    engine = manager.getEngineByName("python");
                    if (engine == null) {
                        throw new UnsupportedOperationException(
                                "Python engine not available. Add Jython dependency.");
                    }
                    break;
                case "groovy":
                    engine = manager.getEngineByName("groovy");
                    if (engine == null) {
                        throw new UnsupportedOperationException(
                                "Groovy engine not available. Add Groovy dependency.");
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Language not supported: " + language);
            }

            // Provide input variables to script
            input.forEach(engine::put);

            // Prepare output container
            Map<String, Object> output = new HashMap<>();
            engine.put("output", output);

            // Execute script
            engine.eval(script);

            // Return output
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) engine.get("output");
            return result != null ? result : output;

        } catch (Exception e) {
            LOG.errorf(e, "Code execution failed");
            throw new RuntimeException("Code execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Workflow.Node.NodeType getSupportedType() {
        return Workflow.Node.NodeType.CODE_EXECUTION;
    }
}