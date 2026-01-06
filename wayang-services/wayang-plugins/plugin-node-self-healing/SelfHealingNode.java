
/**
 * Self-Healing Node - Auto-repair errors using LLM
 * Attempts to fix malformed inputs or outputs
 */
@ApplicationScoped
@NodeType("builtin.self.heal")
public class SelfHealingNode extends AbstractNode {
    
    @Inject
    ModelRouterClient modelRouter;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var errorData = context.getInput("error");
        var schema = config.getObject("targetSchema", Map.class);
        var maxAttempts = config.getInt("maxAttempts", 2);
        
        var prompt = buildRepairPrompt(errorData, schema);
        
        var request = LLMRequest.builder()
            .prompt(prompt)
            .maxTokens(512)
            .temperature(0.1) // Low temperature for deterministic repair
            .build();
        
        return modelRouter.call(request)
            .map(response -> {
                var fixed = response.getOutput();
                
                // Validate fixed output against schema
                var errors = SchemaValidator.validate(fixed, schema);
                if (!errors.isEmpty()) {
                    return ExecutionResult.failed("Auto-fix failed validation");
                }
                
                return ExecutionResult.success(Map.of(
                    "fixed", fixed,
                    "original", errorData,
                    "tokensUsed", response.getTokensUsed()
                ));
            });
    }
    
    private Prompt buildRepairPrompt(Object errorData, Map<String, Object> schema) {
        return Prompt.builder()
            .system("You are a data repair assistant. Fix the provided data to match the schema.")
            .user(String.format(
                "Data: %s\n\nSchema: %s\n\nProvide corrected JSON only.",
                JsonUtils.toJson(errorData),
                JsonUtils.toJson(schema)
            ))
            .build();
    }
}