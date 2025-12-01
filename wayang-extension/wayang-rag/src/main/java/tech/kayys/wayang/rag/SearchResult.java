@Value
@Builder
public class SearchResult {
    String query;
    List<ScoredChunk> results;
    int totalResults;
    Duration searchTime;
}