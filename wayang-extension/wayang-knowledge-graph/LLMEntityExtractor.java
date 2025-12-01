@ApplicationScoped
public class LLMEntityExtractor {
    @Inject LLMRuntime llmRuntime;
    
    public List<Entity> extract(String text) {
        String prompt = buildExtractionPrompt(text);
        
        LLMRequest request = LLMRequest.builder()
            .prompt(Prompt.of(prompt))
            .type(LLMRequestType.COMPLETION)
            .build();
        
        LLMResponse response = llmRuntime.complete(request).join();
        
        return parseEntities(response.getContent());
    }
}

