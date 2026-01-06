
/**
 * RAG node - performs retrieval augmented generation
 */
public class RAGNode extends AbstractNode {
    private final RAGService ragService;
    private final EmbeddingService embeddingService;
    
    public RAGNode(String nodeId, NodeDescriptor descriptor,
                  RAGService ragService, EmbeddingService embeddingService) {
        super(nodeId, descriptor);
        this.ragService = requireNonNull(ragService);
        this.embeddingService = requireNonNull(embeddingService);
    }
    
    @Override
    protected ExecutionResult doExecute(NodeContext context) throws Exception {
        String query = context.getInput("query", String.class);
        int topK = context.getInput("topK", Integer.class);
        
        // Generate query embedding
        float[] queryEmbedding = embeddingService.embed(query);
        
        // Retrieve relevant documents
        List<Document> documents = ragService.search(queryEmbedding, topK);
        
        // Build context from documents
        String retrievedContext = documents.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));
        
        return ExecutionResult.success(Map.of(
                "context", retrievedContext,
                "documents", documents
        ));
    }
}