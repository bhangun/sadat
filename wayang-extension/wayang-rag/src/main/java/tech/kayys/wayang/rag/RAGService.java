
public interface RAGService {
    void ingest(Document document);
    void ingest(List<Document> documents);
    SearchResult search(SearchQuery query);
    void delete(String documentId);
    void reindex();
}



/**
 * RAG Service - orchestrates retrieval augmented generation
 * 
 * Design principles:
 * - Hybrid retrieval (semantic + lexical)
 * - Pluggable vector stores
 * - Re-ranking support
 * - Context assembly
 * - Provenance tracking
 */
@ApplicationScoped
public class RAGService {
    private final IngestionPipeline ingestionPipeline;
    private final RetrievalEngine retrievalEngine;
    private final ContextAssembler contextAssembler;
    private final ProvenanceLogger provenanceLogger;
    private final RAGMetrics metrics;
    
    @Inject
    public RAGService(IngestionPipeline ingestionPipeline,
                     RetrievalEngine retrievalEngine,
                     ContextAssembler contextAssembler,
                     ProvenanceLogger provenanceLogger) {
        this.ingestionPipeline = ingestionPipeline;
        this.retrievalEngine = retrievalEngine;
        this.contextAssembler = contextAssembler;
        this.provenanceLogger = provenanceLogger;
        this.metrics = new RAGMetrics();
    }
    
    /**
     * Ingest documents into RAG system
     */
    public IngestionResult ingest(IngestionRequest request) {
        metrics.recordIngestionStart();
        
        try {
            // Ingest documents
            IngestionResult result = ingestionPipeline.ingest(request);
            
            // Log provenance
            provenanceLogger.logIngestion(request, result);
            
            metrics.recordIngestionSuccess(result);
            return result;
            
        } catch (Exception e) {
            metrics.recordIngestionFailure();
            throw new RAGException("Ingestion failed", e);
        }
    }
    
    /**
     * Search for relevant documents
     */
    public RetrievalResult search(RetrievalRequest request) {
        metrics.recordSearchStart();
        
        try {
            // Retrieve documents
            RetrievalResult result = retrievalEngine.retrieve(request);
            
            // Log provenance
            provenanceLogger.logRetrieval(request, result);
            
            metrics.recordSearchSuccess(result);
            return result;
            
        } catch (Exception e) {
            metrics.recordSearchFailure();
            throw new RAGException("Search failed", e);
        }
    }
    
    /**
     * Retrieve and assemble context for LLM
     */
    public RAGContext retrieveContext(String query, RAGConfig config) {
        // Search for relevant documents
        RetrievalRequest request = RetrievalRequest.builder()
                .query(query)
                .topK(config.getTopK())
                .filters(config.getFilters())
                .hybridWeight(config.getHybridWeight())
                .build();
        
        RetrievalResult result = search(request);
        
        // Assemble context from retrieved documents
        return contextAssembler.assemble(result.getDocuments(), config);
    }
}
