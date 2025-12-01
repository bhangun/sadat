public interface LLMRuntime {
    CompletableFuture<LLMResponse> complete(LLMRequest request);
    Publisher<LLMChunk> stream(LLMRequest request);
    CompletableFuture<float[]> embed(String text);
}