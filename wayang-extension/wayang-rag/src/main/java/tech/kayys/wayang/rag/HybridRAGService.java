

@ApplicationScoped
public class HybridRAGService implements RAGService {
    @Inject IngestionPipeline ingestionPipeline;
    @Inject EmbeddingService embeddingService;
    @Inject VectorStore vectorStore;
    @Inject MetadataStore metadataStore;
    @Inject SearchEngine searchEngine;
    @Inject ReRanker reRanker;
    
    @Override
    public void ingest(Document document) {
        // Extract text and metadata
        ProcessedDocument processed = ingestionPipeline.process(document);
        
        // Chunk document
        List<Chunk> chunks = ingestionPipeline.chunk(processed);
        
        // Generate embeddings
        List<EmbeddedChunk> embedded = chunks.stream()
            .map(chunk -> new EmbeddedChunk(
                chunk,
                embeddingService.embed(chunk.getText())
            ))
            .collect(Collectors.toList());
        
        // Store
        vectorStore.store(embedded);
        metadataStore.store(document.getId(), processed.getMetadata());
    }
    
    @Override
    public SearchResult search(SearchQuery query) {
        // Hybrid search: semantic + lexical
        List<ScoredChunk> semanticResults = vectorStore.search(
            embeddingService.embed(query.getQuery()),
            query.getTopK() * 2  // Get more for re-ranking
        );
        
        List<ScoredChunk> lexicalResults = searchEngine.search(
            query.getQuery(),
            query.getTopK() * 2
        );
        
        // Fusion
        List<ScoredChunk> fused = fuseResults(
            semanticResults,
            lexicalResults,
            query.getSemanticWeight()
        );
        
        // Re-rank
        List<ScoredChunk> reranked = reRanker.rerank(
            fused,
            query.getQuery()
        );
        
        // Take top-k
        List<ScoredChunk> topK = reranked.stream()
            .limit(query.getTopK())
            .collect(Collectors.toList());
        
        return SearchResult.builder()
            .query(query.getQuery())
            .results(topK)
            .totalResults(fused.size())
            .build();
    }
}