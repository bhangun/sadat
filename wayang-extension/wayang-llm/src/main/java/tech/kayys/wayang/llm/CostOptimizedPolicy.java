@ApplicationScoped
class CostOptimizedPolicy implements RoutingPolicy {
    @Override
    public ModelDescriptor select(List<ModelDescriptor> models, LLMRequest request) {
        return models.stream()
            .min(Comparator.comparing(ModelDescriptor::costPerToken))
            .orElseThrow();
    }
}