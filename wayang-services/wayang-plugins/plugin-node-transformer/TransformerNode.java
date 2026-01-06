
/**
 * ==============================================
 * DATA TRANSFORMATION NODES
 * ==============================================
 */

/**
 * Transformer Node - Transform data using templates or scripts
 * Supports JSONPath, JOLT, and JavaScript transformations
 */
@ApplicationScoped
@NodeType("builtin.transform")
public class TransformerNode extends AbstractNode {
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var input = context.getInput("data");
        var type = config.getString("type", "jsonpath");
        var template = config.getString("template");
        
        return Uni.createFrom().item(() -> {
            var transformed = switch (type) {
                case "jsonpath" -> applyJsonPath(input, template);
                case "jolt" -> applyJolt(input, template);
                case "javascript" -> applyJavaScript(input, template);
                default -> throw new ValidationException("Unknown transform type: " + type);
            };
            
            return ExecutionResult.success(Map.of("result", transformed));
        });
    }
    
    private Object applyJsonPath(Object input, String path) {
        return JsonPath.read(input, path);
    }
    
    private Object applyJolt(Object input, String spec) {
        var chainr = Chainr.fromSpec(JsonUtils.jsonToObject(spec));
        return chainr.transform(input);
    }
    
    private Object applyJavaScript(Object input, String script) {
        var engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.put("input", input);
        try {
            return engine.eval(script);
        } catch (ScriptException e) {
            throw new ValidationException("JavaScript execution failed", e);
        }
    }
}
