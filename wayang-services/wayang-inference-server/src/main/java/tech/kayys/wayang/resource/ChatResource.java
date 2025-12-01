package tech.kayys.wayang.resource;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.engine.LlamaConfig.SamplingConfig;
import tech.kayys.wayang.model.ChatMessage;
import tech.kayys.wayang.model.ChatRequest;
import tech.kayys.wayang.model.ChatResponse;
import tech.kayys.wayang.model.GenerationResult;
import tech.kayys.wayang.model.StreamChunk;
import tech.kayys.wayang.service.EngineService;

public class ChatResource {
    
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
    @Inject
    EngineService engineService;
    
    @POST
    @Path("/completions")
    public Object completions(ChatRequest request) {
        if (Boolean.TRUE.equals(request.stream())) {
            return streamCompletions(request);
        }
        
        String id = "chatcmpl-" + requestCounter.incrementAndGet();
        long created = System.currentTimeMillis() / 1000;
        
        SamplingConfig config = createSamplingConfig(request);
        GenerationResult result = engineService.chat(request.messages(), config, 
            request.maxTokens() != null ? request.maxTokens() : 512, null);
        
        return new ChatResponse(
            id, "chat.completion", created, engineService.getModelInfo(),
            new ChatResponse.Choice(0, 
                new ChatMessage("assistant", result.text()),
                result.finishReason()),
            new ChatResponse.Usage(result.promptTokens(), result.tokensGenerated(),
                result.promptTokens() + result.tokensGenerated())
        );
    }
    
    @POST
    @Path("/completions")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> streamCompletions(ChatRequest request) {
        String id = "chatcmpl-" + requestCounter.incrementAndGet();
        long created = System.currentTimeMillis() / 1000;
        
        return Multi.createFrom().emitter(emitter -> {
            try {
                emitter.emit(formatStreamChunk(id, created, "assistant", null, null));
                
                SamplingConfig config = createSamplingConfig(request);
                engineService.chat(request.messages(), config,
                    request.maxTokens() != null ? request.maxTokens() : 512,
                    piece -> emitter.emit(formatStreamChunk(id, created, null, piece, null)));
                
                emitter.emit(formatStreamChunk(id, created, null, null, "stop"));
                emitter.emit("data: [DONE]\n\n");
                emitter.complete();
            } catch (Exception e) {
                emitter.fail(e);
            }
        });
    }
    
    private SamplingConfig createSamplingConfig(ChatRequest request) {
        return SamplingConfig.builder()
            .temperature(request.temperature() != null ? request.temperature() : 0.8f)
            .topK(request.topK() != null ? request.topK() : 40)
            .topP(request.topP() != null ? request.topP() : 0.95f)
            .minP(request.minP() != null ? request.minP() : 0.05f)
            .repeatPenalty(request.repeatPenalty() != null ? request.repeatPenalty() : 1.1f)
            .presencePenalty(request.presencePenalty() != null ? request.presencePenalty() : 0.0f)
            .frequencyPenalty(request.frequencyPenalty() != null ? request.frequencyPenalty() : 0.0f)
            .build();
    }
    
    private String formatStreamChunk(String id, long created, String role, String content, String finishReason) {
        try {
            var delta = role != null ? 
                new StreamChunk.Delta(role, null) : 
                new StreamChunk.Delta(null, content);
            
            var chunk = new StreamChunk(id, "chat.completion.chunk", created, 
                engineService.getModelInfo(), delta, finishReason);
            
            return "data: " + new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(chunk) + "\n\n";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
