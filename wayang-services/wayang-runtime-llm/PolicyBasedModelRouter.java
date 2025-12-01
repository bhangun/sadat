@ApplicationScoped
public class PolicyBasedModelRouter {
    @Inject ModelRegistry modelRegistry;
    @Inject RoutingPolicyEngine routingPolicyEngine;
    
    public ModelSelection selectModel(LLMRequest request) {
        // Get available models
        List<ModelInfo> candidates = modelRegistry.getModels(
            request.getCapabilities()
        );
        
        // Apply routing policy
        RoutingPolicy policy = routingPolicyEngine.getPolicy(
            request.getTenantId()
        );
        
        // Score candidates
        List<ScoredModel> scored = candidates.stream()
            .map(model -> new ScoredModel(
                model,
                scoreModel(model, request, policy)
            ))
            .collect(Collectors.toList());
        
        // Select best
        ModelInfo selected = scored.stream()
            .max(Comparator.comparing(ScoredModel::getScore))
            .map(ScoredModel::getModel)
            .orElseThrow(() -> new NoModelAvailableException());
        
        return ModelSelection.builder()
            .modelId(selected.getId())
            .providerId(selected.getProviderId())
            .endpoint(selected.getEndpoint())
            .build();
    }
    
    private double scoreModel(ModelInfo model, LLMRequest request, RoutingPolicy policy) {
        double score = 0.0;
        
        // Latency score
        if (request.getLatencyBudget() != null) {
            score += policy.getLatencyWeight() * (1.0 / model.getLatencyP95().toMillis());
        }
        
        // Cost score
        score += policy.getCostWeight() * (1.0 / model.getCostPerToken());
        
        // Quality score
        score += policy.getQualityWeight() * model.getQualityScore();
        
        return score;
    }
}