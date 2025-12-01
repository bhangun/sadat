@ApplicationScoped
@Named("react")
public class ReActStrategy implements PlanningStrategy {
    
    @Inject
    LLMClient llmClient;
    
    @Inject
    ToolRegistry toolRegistry;
    
    @Override
    public String name() {
        return "react";
    }
    
    @Override
    public Uni<List<Task>> decompose(Goal goal) {
        // ReAct: Reasoning + Acting pattern
        return toolRegistry.getAvailableTools()
            .flatMap(tools -> {
                String prompt = buildReActPrompt(goal, tools);
                return llmClient.complete(prompt);
            })
            .map(response -> parseReActTasks(response));
    }
    
    @Override
    public Uni<List<Task>> revise(
        ExecutionPlan existing, 
        List<Task> failed, 
        PlanningContext context
    ) {
        return toolRegistry.getAvailableTools()
            .flatMap(tools -> {
                String prompt = buildReActRevisionPrompt(existing, failed, tools, context);
                return llmClient.complete(prompt);
            })
            .map(response -> parseReActTasks(response));
    }
    
    private String buildReActPrompt(Goal goal, List<ToolDescriptor> tools) {
        return """
            You are an agent that reasons and acts. For the given goal, determine:
            1. What information you need (Thought)
            2. What action/tool to use (Action)
            3. What to expect (Observation)
            
            Goal: %s
            
            Available Tools:
            %s
            
            Provide a reasoning chain in this format:
            Thought: [your reasoning]
            Action: [tool_name with parameters]
            Expected Observation: [what you expect to learn]
            """.formatted(goal.description(), formatTools(tools));
    }
    
    private String formatTools(List<ToolDescriptor> tools) {
        return tools.stream()
            .map(t -> "- " + t.name() + ": " + t.description())
            .collect(Collectors.joining("\n"));
    }
    
    private List<Task> parseReActTasks(String response) {
        // Parse ReAct reasoning chain
        return List.of(); // Placeholder
    }
}