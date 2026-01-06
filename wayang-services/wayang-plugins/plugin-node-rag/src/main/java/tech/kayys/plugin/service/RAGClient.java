
/**
 * ==============================================
 * RAG CLIENT - Retrieval Augmented Generation
 * ==============================================
 */
@ApplicationScoped
public class RAGClient {
    
    @Inject
    @RestClient
    RAGService ragService;
    
    @Inject
    ModelRouterClient modelRouter;
    
    @Inject
    ReRanker reRanker;
    
    /**
     * Retrieve relevant documents
     */
    public Uni<RAGResponse> retrieve(RAGRequest request) {
        return Uni.createFrom().item(() -> {
            // Generate query embedding
            return modelRouter.embed(EmbedRequest.builder()
                .texts(List.of(request.getQuery()))
                .build());
        })
        .flatMap(embedResponse -> {
            var queryEmbedding = embedResponse.getEmbeddings().get(0);
            
            // Perform hybrid search if enabled
            if (request.isHybrid()) {
                return performHybridSearch(request, queryEmbedding);
            } else {
                return performVectorSearch(request, queryEmbedding);
            }
        })
        .flatMap(results -> {
            // Re-rank if enabled
            if (request.isRerank() && results.size() > request.getTopK()) {
                return reRanker.rerank(request.getQuery(), results)
                    .map(reranked -> reranked.subList(0, Math.min(request.getTopK(), reranked.size())));
            }
            return Uni.createFrom().item(results);
        })
        .map(finalResults -> {
            // Build response with citations
            var response = new RAGResponse();
            response.setDocuments(finalResults);
            response.setScores(finalResults.stream()
                .map(Document::getScore)
                .collect(Collectors.toList()));
            response.setContext(assembleContext(finalResults));
            response.setCitations(buildCitations(finalResults));
            return response;
        });
    }
    
    private Uni<List<Document>> performHybridSearch(RAGRequest request, List<Float> embedding) {
        // Combine vector and keyword search
        return Uni.combine().all().unis(
            performVectorSearch(request, embedding),
            performKeywordSearch(request)
        ).asTuple()
        .map(tuple -> {
            var vectorResults = tuple.getItem1();
            var keywordResults = tuple.getItem2();
            
            // Merge and deduplicate
            return mergeResults(vectorResults, keywordResults, request.getTopK());
        });
    }
    
    private Uni<List<Document>> performVectorSearch(RAGRequest request, List<Float> embedding) {
        var searchRequest = new VectorSearchRequest();
        searchRequest.setIndex(request.getIndex());
        searchRequest.setEmbedding(embedding);
        searchRequest.setTopK(request.getTopK() * 2); // Over-fetch for re-ranking
        searchRequest.setFilters(request.getFilters());
        
        return ragService.vectorSearch(searchRequest)
            .map(response -> response.getDocuments());
    }
    
    private Uni<List<Document>> performKeywordSearch(RAGRequest request) {
        var searchRequest = new KeywordSearchRequest();
        searchRequest.setIndex(request.getIndex());
        searchRequest.setQuery(request.getQuery());
        searchRequest.setTopK(request.getTopK());
        searchRequest.setFilters(request.getFilters());
        
        return ragService.keywordSearch(searchRequest)
            .map(response -> response.getDocuments());
    }
    
    private List<Document> mergeResults(List<Document> vector, List<Document> keyword, int topK) {
        // Reciprocal Rank Fusion (RRF)
        var scoreMap = new HashMap<String, Double>();
        
        for (int i = 0; i < vector.size(); i++) {
            var doc = vector.get(i);
            scoreMap.put(doc.getId(), scoreMap.getOrDefault(doc.getId(), 0.0) + 1.0 / (i + 60));
        }
        
        for (int i = 0; i < keyword.size(); i++) {
            var doc = keyword.get(i);
            scoreMap.put(doc.getId(), scoreMap.getOrDefault(doc.getId(), 0.0) + 1.0 / (i + 60));
        }
        
        // Merge unique documents
        var allDocs = new HashMap<String, Document>();
        vector.forEach(doc -> allDocs.put(doc.getId(), doc));
        keyword.forEach(doc -> allDocs.put(doc.getId(), doc));
        
        // Sort by RRF score
        return allDocs.values().stream()
            .sorted((a, b) -> Double.compare(
                scoreMap.getOrDefault(b.getId(), 0.0),
                scoreMap.getOrDefault(a.getId(), 0.0)
            ))
            .limit(topK)
            .collect(Collectors.toList());
    }
    
    private String assembleContext(List<Document> documents) {
        var context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            var doc = documents.get(i);
            context.append(String.format("[%d] %s\n\n", i + 1, doc.getContent()));
        }
        return context.toString();
    }
    
    private List<Citation> buildCitations(List<Document> documents) {
        return documents.stream()
            .map(doc -> new Citation(
                doc.getId(),
                doc.getSource(),
                doc.getMetadata().get("title"),
                doc.getScore()
            ))
            .collect(Collectors.toList());
    }
}
