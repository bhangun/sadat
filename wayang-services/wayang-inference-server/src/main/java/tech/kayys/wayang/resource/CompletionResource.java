package tech.kayys.wayang.resource;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.engine.LlamaConfig.SamplingConfig;
import tech.kayys.wayang.model.CompletionRequest;
import tech.kayys.wayang.model.CompletionResponse;
import tech.kayys.wayang.model.GenerationResult;
import tech.kayys.wayang.plugin.ModelManager;

public class CompletionResource {
    private static final Logger log = Logger.getLogger(CompletionResource.class);
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
    @Inject
    ModelManager modelManager;
    
    @POST
    public Object createCompletion(CompletionRequest request) {
        if (Boolean.TRUE.equals(request.stream())) {
            return streamCompletion(request);
        }
        
        String id = "cmpl-" + requestCounter.incrementAndGet();
        long created = System.currentTimeMillis() / 1000;
        
        log.infof("Completion request: prompt length=%d", request.prompt().length());
        
        try {
            SamplingConfig config = SamplingConfig.builder()
                .temperature(request.temperature() != null ? request.temperature() : 0.8f)
                .topK(request.topK() != null ? request.topK() : 40)
                .topP(request.topP() != null ? request.topP() : 0.95f)
                .build();
            
            GenerationResult result = modelManager.getActiveModel().generate(
                request.prompt(), config,
                request.maxTokens() != null ? request.maxTokens() : 512,
                request.stop(),
                null
            );
            
            return new CompletionResponse(
                id, "text_completion", created,
                modelManager.getActiveModel().getModelInfo().name(),
                new CompletionResponse.Choice(0, result.text(), result.finishReason()),
                new CompletionResponse.Usage(result.promptTokens(), result.tokensGenerated(),
                    result.promptTokens() + result.tokensGenerated())
            );
            
        } catch (Exception e) {
            log.error("Completion failed", e);
            throw new WebApplicationException("Completion failed: " + e.getMessage(), 500);
        }
    }
    
    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> streamCompletion(CompletionRequest request) {
        String id = "cmpl-" + requestCounter.incrementAndGet();
        long created = System.currentTimeMillis() / 1000;
        
        return Multi.createFrom().emitter(emitter -> {
            try {
                SamplingConfig config = SamplingConfig.builder()
                    .temperature(request.temperature() != null ? request.temperature() : 0.8f)
                    .topK(request.topK() != null ? request.topK() : 40)
                    .topP(request.topP() != null ? request.topP() : 0.95f)
                    .build();
                
                modelManager.getActiveModel().generate(
                    request.prompt(), config,
                    request.maxTokens() != null ? request.maxTokens() : 512,
                    request.stop(),
                    piece -> {
                        try {
                            var chunk = new CompletionResponse(id, "text_completion", created,
                                modelManager.getActiveModel().getModelInfo().name(),
                                new CompletionResponse.Choice(0, piece, null),
                                null);
                            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(chunk);
                            emitter.emit("data: " + json + "\n\n");
                        } catch (Exception e) {
                            emitter.fail(e);
                        }
                    }
                );
                
                emitter.emit("data: [DONE]\n\n");
                emitter.complete();
                
            } catch (Exception e) {
                log.error("Stream completion failed", e);
                emitter.fail(e);
            }
        });
    }
}
