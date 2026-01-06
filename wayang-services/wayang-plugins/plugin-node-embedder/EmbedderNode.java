
/**
 * Embedder Node - Generate embeddings for text
 * Supports batch processing and multiple embedding models
 */
@ApplicationScoped
@NodeType("builtin.embedder")
public class EmbedderNode extends AbstractNode {
    
    @Inject
    ModelRouterClient modelRouter;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var texts = (Collection<String>) context.getInput("texts");
        var model = config.getString("model", "default");
        
        if (texts == null || texts.isEmpty()) {
            return Uni.createFrom().item(ExecutionResult.success(Map.of("embeddings", List.of())));
        }
        
        var request = EmbedRequest.builder()
            .texts(new ArrayList<>(texts))
            .model(model)
            .normalize(config.getBoolean("normalize", true))
            .build();
        
        return modelRouter.embed(request)
            .map(response -> ExecutionResult.success(Map.of(
                "embeddings", response.getEmbeddings(),
                "dimensions", response.getDimensions(),
                "model", response.getModel()
            )));
    }
}
