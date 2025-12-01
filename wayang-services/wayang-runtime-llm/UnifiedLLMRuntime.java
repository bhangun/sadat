
@ApplicationScoped
public class UnifiedLLMRuntime implements LLMRuntime {
    @Inject ModelRouter modelRouter;
    @Inject PromptShaper promptShaper;
    @Inject SafetyGate safetyGate;
    @Inject ResponseCache responseCache;
    @Inject ProviderRegistry providerRegistry;
    @Inject CostCalculator costCalculator;
    
    @Override
    public CompletableFuture<LLMResponse> complete(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Span span = startSpan(request);
            
            try {
                // Safety pre-check
                SafetyResult preCheck = safetyGate.preCheck(request);
                if (!preCheck.isAllowed()) {
                    throw new SafetyViolationException(preCheck);
                }
                
                // Check cache
                Optional<LLMResponse> cached = responseCache.get(request);
                if (cached.isPresent()) {
                    return cached.get();
                }
                
                // Shape prompt
                ShapedPrompt shaped = promptShaper.shape(request);
                
                // Route to model
                ModelSelection selection = modelRouter.selectModel(request);
                
                // Get provider
                LLMProvider provider = providerRegistry.getProvider(
                    selection.getProviderId()
                );
                
                // Execute
                LLMResponse response = provider.complete(shaped, selection);
                
                // Safety post-check
                SafetyResult postCheck = safetyGate.postCheck(request, response);
                if (!postCheck.isAllowed()) {
                    throw new SafetyViolationException(postCheck);
                }
                
                // Calculate cost
                double cost = costCalculator.calculate(response);
                response = response.withCost(cost);
                
                // Cache if eligible
                if (request.isCacheable()) {
                    responseCache.put(request, response);
                }
                
                return response;
                
            } finally {
                span.end();
            }
        });
    }
}
