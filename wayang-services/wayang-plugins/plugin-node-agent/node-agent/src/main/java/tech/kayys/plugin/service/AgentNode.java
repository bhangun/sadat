package tech.kayys.plugin.service;

import tech.kayys.wayang.plugin.node.NodeContext;

/**
 * Agent Node - LLM-powered reasoning with chain-of-thought
 * Supports function calling, streaming, and persona injection
 */
@ApplicationScoped
@NodeType("builtin.agent")
public class AgentNode extends AbstractNode {
    
    @Inject
    ModelRouterClient modelRouter;
    
    @Inject
    PersonaService personaService;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var prompt = buildPrompt(context);
        var modelHints = config.getObject("modelHints", Map.class);
        var functions = config.getList("functions", Map.class);
        
        var request = LLMRequest.builder()
            .prompt(prompt)
            .modelHints(modelHints)
            .functions(functions)
            .maxTokens(config.getInt("maxTokens", 1024))
            .temperature(config.getDouble("temperature", 0.7))
            .stream(config.getBoolean("stream", false))
            .metadata(Map.of(
                "runId", context.getRunId(),
                "nodeId", context.getNodeId(),
                "tenantId", context.getTenantId()
            ))
            .build();
        
        return modelRouter.call(request)
            .map(response -> ExecutionResult.success(Map.of(
                "response", response.getOutput(),
                "model", response.getModelId(),
                "tokensUsed", response.getTokensUsed(),
                "cost", response.getCost(),
                "functionCalls", response.getFunctionCalls()
            )));
    }
    
    private Prompt buildPrompt(NodeContext context) {
        var personaId = config.getString("persona");
        var persona = personaId != null 
            ? personaService.getPersona(personaId) 
            : Persona.defaultPersona();
        
        var systemPrompt = persona.getSystemPrompt();
        var userMessage = context.getInput("message");
        
        return Prompt.builder()
            .system(systemPrompt)
            .user(userMessage)
            .context(buildContext(context))
            .build();
    }
    
    private Map<String, Object> buildContext(NodeContext context) {
        var ctx = new HashMap<String, Object>();
        
        // Add RAG context if available
        var ragContext = context.getInput("ragContext");
        if (ragContext != null) {
            ctx.put("knowledge", ragContext);
        }
        
        // Add memory context
        var memoryContext = context.getInput("memory");
        if (memoryContext != null) {
            ctx.put("memory", memoryContext);
        }
        
        return ctx;
    }
}
