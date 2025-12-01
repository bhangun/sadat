@Value
@Builder
public class GraphQuery {
    String cypherQuery;
    Map<String, Object> parameters;
    int limit;
}
