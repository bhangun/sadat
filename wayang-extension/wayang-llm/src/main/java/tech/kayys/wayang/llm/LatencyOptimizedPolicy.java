
@ApplicationScoped
class LatencyOptimizedPolicy implements RoutingPolicy {
    @Override
    public ModelDescriptor select(List<ModelDescriptor> models, LLMRequest request) {
        return models.stream()
            .min(Comparator.comparing(m -> m.latencyProfile().p95()))
            .orElseThrow();
    }
}