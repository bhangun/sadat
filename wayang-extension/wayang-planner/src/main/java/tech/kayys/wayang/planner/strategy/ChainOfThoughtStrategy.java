@ApplicationScoped
@Named("chain-of-thought")
public class ChainOfThoughtStrategy implements PlanningStrategy {
    
    @Inject
    LLMClient llmClient;
    
    @Override
    public String name() {
        return "chain-of-thought";
    }
    
    @Override
    public Uni<List<Task>> decompose(Goal goal) {
        String prompt = buildCOTPrompt(goal);
        
        return llmClient.complete(prompt)
            .map(response -> parseTasksFromResponse(response));
    }
    
    @Override
    public Uni<List<Task>> revise(
        ExecutionPlan existing, 
        List<Task> failed, 
        PlanningContext context
    ) {
        String prompt = buildRevisionPrompt(existing, failed, context);
        
        return llmClient.complete(prompt)
            .map(response -> parseTasksFromResponse(response));
    }
    
    private String buildCOTPrompt(Goal goal) {
        return """
            You are a strategic planning assistant. Break down the following goal into 
            a sequence of concrete, actionable tasks. Think step-by-step.
            
            Goal: %s
            
            Context: %s
            
            Provide tasks in JSON format with the following structure:
            [
              {
                "id": "task-1",
                "description": "Task description",
                "type": "data_fetch|analysis|generation|validation",
                "dependsOn": []
              }
            ]
            """.formatted(goal.description(), goal.context());
    }
    
    private List<Task> parseTasksFromResponse(String response) {
        // Parse LLM response and convert to Task objects
        // Implementation would use JSON parsing
        return List.of(); // Placeholder
    }
}