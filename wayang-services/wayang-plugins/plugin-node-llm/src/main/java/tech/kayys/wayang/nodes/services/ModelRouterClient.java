package tech.kayys.wayang.nodes.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Tracer;

/**
 * ==============================================
 * MODEL ROUTER CLIENT - LLM Inference
 * Unified interface for calling any LLM provider
 * ==============================================
 */
@ApplicationScoped
public class ModelRouterClient {
    
    @Inject
    @RestClient
    ModelRouterService modelService;
    
    @Inject
    ModelCapabilityRegistry capabilityRegistry;
    
    @Inject
    PromptShaper promptShaper;
    
    @Inject
    CostCalculator costCalculator;
    
    @Inject
    Tracer tracer;
    
    /**
     * Execute LLM call with automatic model selection
     */
    public Uni<LLMResponse> call(LLMRequest request) {
        var span = tracer.spanBuilder("llm.call").startSpan();
        
        return Uni.createFrom().item(() -> {
            // Select best model based on hints
            var model = selectModel(request.getModelHints());
            span.setAttribute("model.id", model.getId());
            
            // Shape prompt with context
            var shapedPrompt = promptShaper.shape(
                request.getPrompt(),
                model.getMaxTokens(),
                request.getMetadata()
            );
            
            return new SelectedModel(model, shapedPrompt);
        })
        .flatMap(selected -> {
            // Call model service
            var apiRequest = buildApiRequest(selected, request);
            return modelService.complete(apiRequest);
        })
        .map(apiResponse -> {
            // Parse and enrich response
            var response = parseResponse(apiResponse);
            response.setCost(costCalculator.calculate(
                response.getModelId(),
                response.getTokensIn(),
                response.getTokensOut()
            ));
            
            span.setAttribute("tokens.in", response.getTokensIn());
            span.setAttribute("tokens.out", response.getTokensOut());
            span.setAttribute("cost", response.getCost());
            
            return response;
        })
        .eventually(() -> span.end());
    }
    
    /**
     * Generate embeddings
     */
    public Uni<EmbedResponse> embed(EmbedRequest request) {
        return Uni.createFrom().item(() -> {
            var model = request.getModel() != null 
                ? capabilityRegistry.getModel(request.getModel())
                : capabilityRegistry.getDefaultEmbeddingModel();
            
            return model;
        })
        .flatMap(model -> {
            var apiRequest = new EmbedApiRequest();
            apiRequest.setModel(model.getId());
            apiRequest.setTexts(request.getTexts());
            apiRequest.setNormalize(request.isNormalize());
            
            return modelService.embed(apiRequest);
        })
        .map(apiResponse -> {
            var response = new EmbedResponse();
            response.setEmbeddings(apiResponse.getEmbeddings());
            response.setDimensions(apiResponse.getDimensions());
            response.setModel(apiResponse.getModel());
            return response;
        });
    }
    
    /**
     * Select best model based on hints and availability
     */
    private ModelInfo selectModel(Map<String, Object> hints) {
        if (hints == null || hints.isEmpty()) {
            return capabilityRegistry.getDefaultModel();
        }
        
        // Check for preferred models
        var preferred = (List<String>) hints.get("preferred");
        if (preferred != null) {
            for (var modelId : preferred) {
                var model = capabilityRegistry.getModel(modelId);
                if (model != null && model.isAvailable()) {
                    return model;
                }
            }
        }
        
        // Check for required capabilities
        var capabilities = (List<String>) hints.get("capabilities");
        if (capabilities != null) {
            return capabilityRegistry.findBestModel(capabilities);
        }
        
        return capabilityRegistry.getDefaultModel();
    }
    
    private ModelApiRequest buildApiRequest(SelectedModel selected, LLMRequest request) {
        var apiRequest = new ModelApiRequest();
        apiRequest.setModel(selected.getModel().getId());
        apiRequest.setMessages(selected.getPrompt().toMessages());
        apiRequest.setMaxTokens(request.getMaxTokens());
        apiRequest.setTemperature(request.getTemperature());
        apiRequest.setStream(request.isStream());
        
        // Add functions if supported
        if (request.getFunctions() != null && !request.getFunctions().isEmpty()) {
            if (selected.getModel().supportsFunction()) {
                apiRequest.setFunctions(request.getFunctions());
            }
        }
        
        return apiRequest;
    }
    
    private LLMResponse parseResponse(ModelApiResponse apiResponse) {
        var response = new LLMResponse();
        response.setModelId(apiResponse.getModel());
        response.setOutput(apiResponse.getContent());
        response.setTokensIn(apiResponse.getUsage().getPromptTokens());
        response.setTokensOut(apiResponse.getUsage().getCompletionTokens());
        response.setFunctionCalls(apiResponse.getFunctionCalls());
        return response;
    }
    
    private static class SelectedModel {
        private final ModelInfo model;
        private final Prompt prompt;
        
        public SelectedModel(ModelInfo model, Prompt prompt) {
            this.model = model;
            this.prompt = prompt;
        }
        
        public ModelInfo getModel() { return model; }
        public Prompt getPrompt() { return prompt; }
    }
}
