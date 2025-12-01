interface RoutingPolicy {
    ModelDescriptor select(List<ModelDescriptor> models, LLMRequest request);
}
