package tech.kayys.wayang.models.service;

import tech.kayys.wayang.models.client.*;
import tech.kayys.wayang.models.registry.ModelRegistry;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ModelRouterService {
    
    @Inject
    ModelRegistry modelRegistry;
    
    @Inject
    Map<String, LLMProvider> providers;
    
    @Inject
    CostCalculator costCalculator;
    
    @Inject
    ModelCache modelCache;
    
    public Uni<LLMResponse> complete(LLMRequest request, RoutingPolicy policy) {
        return selectModel(request, policy)
            .flatMap(model -> {
                // Check cache first
                String cacheKey = buildCacheKey(request, model);
                
                return modelCache.get(cacheKey)
                    .onItem().ifNull().switchTo(() -> 
                        executeWithProvider(request, model)
                            .invoke(response -> 
                                modelCache.put(cacheKey, response, model.cacheT TL())
                            )
                    );
            });
    }
    
    public Multi<String> stream(LLMRequest request, RoutingPolicy policy) {
        return selectModel(request, policy)
            .toMulti()
            .flatMap(model -> {
                LLMProvider provider = providers.get(model.provider());
                return provider.stream(request, model);
            });
    }
    
    private Uni<ModelDescriptor> selectModel(LLMRequest request, RoutingPolicy policy) {
        return modelRegistry.findModels(request.capabilities())
            .map(models -> {
                // Apply routing policy
                return policy.select(models, request);
            });
    }
    
    private Uni<LLMResponse> executeWithProvider(
        LLMRequest request, 
        ModelDescriptor model
    ) {
        LLMProvider provider = providers.get(model.provider());
        
        long startTime = System.currentTimeMillis();
        
        return provider.complete(request, model)
            .invoke(response -> {
                long duration = System.currentTimeMillis() - startTime;
                
                // Calculate and track cost
                double cost = costCalculator.calculate(
                    model, 
                    response.tokensIn(), 
                    response.tokensOut()
                );
                
                // Emit metrics
                emitMetrics(model, duration, cost, response);
            });
    }
}