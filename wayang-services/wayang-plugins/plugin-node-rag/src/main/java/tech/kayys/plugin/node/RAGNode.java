
/**
 * RAG Node - Retrieval Augmented Generation
 * Performs hybrid search (vector + keyword) with re-ranking
 */
@ApplicationScoped
@NodeType("builtin.rag")
public class RAGNode extends AbstractNode {
    
    @Inject
    RAGClient ragClient;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var query = (String) context.getInput("query");
        var topK = config.getInt("topK", 5);
        var indexName = config.getString("index", "default");
        var useHybrid = config.getBoolean("hybrid", true);
        var rerank = config.getBoolean("rerank", true);
        
        var request = RAGRequest.builder()
            .query(query)
            .topK(topK)
            .index(indexName)
            .hybrid(useHybrid)
            .rerank(rerank)
            .filters(buildFilters(context))
            .build();
        
        return ragClient.retrieve(request)
            .map(response -> ExecutionResult.success(Map.of(
                "documents", response.getDocuments(),
                "scores", response.getScores(),
                "context", response.assembleContext(),
                "citations", response.getCitations()
            )));
    }
    
    private Map<String, Object> buildFilters(NodeContext context) {
        var filters = new HashMap<String, Object>();
        filters.put("tenantId", context.getTenantId());
        
        // Add custom filters from config
        var customFilters = config.getObject("filters", Map.class);
        if (customFilters != null) {
            filters.putAll(customFilters);
        }
        
        return filters;
    }
}
