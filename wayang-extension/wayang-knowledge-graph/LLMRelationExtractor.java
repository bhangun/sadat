
@ApplicationScoped
public class LLMRelationExtractor {
    @Inject LLMRuntime llmRuntime;
    
    public List<Triple> extract(String text, List<Entity> entities) {
        String prompt = buildRelationPrompt(text, entities);
        
        LLMRequest request = LLMRequest.builder()
            .prompt(Prompt.of(prompt))
            .type(LLMRequestType.COMPLETION)
            .build();
        
        LLMResponse response = llmRuntime.complete(request).join();
        
        return parseTriples(response.getContent(), entities);
    }
}