@Value
@Builder
public class TraversalQuery {
    String startEntityId;
    int maxDepth;
    List<String> relationTypes;
    Map<String, Object> filters;
    Map<String, Object> parameters;
}