package tech.kayys.wayang.service;



public class ModelWarmupService {
    private static final Logger log = Logger.getLogger(ModelWarmupService.class);
    
    public void warmupModel(LlamaEngine engine) {
        log.info("Starting model warmup...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Warmup with short prompts
            List<String> warmupPrompts = List.of(
                "Hello",
                "Test",
                "Quick check"
            );
            
            SamplingConfig config = SamplingConfig.builder()
                .temperature(0.7f)
                .build();
            
            for (String prompt : warmupPrompts) {
                engine.generate(prompt, config, 10, null, null);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.infof("Model warmup completed in %dms", duration);
            
        } catch (Exception e) {
            log.error("Model warmup failed", e);
        }
    }
    
    public void warmupChat(LlamaEngine engine) {
        log.info("Warming up chat mode...");
        
        try {
            List<ChatMessage> messages = List.of(
                new ChatMessage("user", "Hi")
            );
            
            engine.chat(messages, SamplingConfig.defaults(), 5, null);
            log.info("Chat warmup completed");
            
        } catch (Exception e) {
            log.error("Chat warmup failed", e);
        }
    }
}
