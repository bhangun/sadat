@Value
@Builder
public class SearchQuery {
    String query;
    int topK;
    double semanticWeight;  // 0.0-1.0, weight for semantic vs lexical
    Map<String, Object> filters;
    UUID tenantId;
}