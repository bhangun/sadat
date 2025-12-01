@ApplicationScoped
@Named("cot")
public class ChainOfThoughtPlanner implements PlanningStrategy {
    @Inject LLMAdapter llmAdapter;
    
    @Override
    public ActionGraph plan(Goal goal, PlanningContext context) {
        // Use LLM with CoT prompting
        String prompt = buildCoTPrompt(goal, context);
        String response = llmAdapter.complete(prompt);
        return parseActionGraph(response);
    }
}
