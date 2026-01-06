@Path("/api/v1/rag")
@RegisterRestClient(configKey = "rag-service")
public interface RAGService {
    
    @POST
    @Path("/vector-search")
    Uni<VectorSearchResponse> vectorSearch(VectorSearchRequest request);
    
    @POST
    @Path("/keyword-search")
    Uni<KeywordSearchResponse> keywordSearch(KeywordSearchRequest request);
}