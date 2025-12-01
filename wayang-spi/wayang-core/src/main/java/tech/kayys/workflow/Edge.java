@Value
@Builder
public class Edge {
    String id;
    String sourceNodeId;
    String sourcePort;
    String targetNodeId;
    String targetPort;
    Optional<String> condition;  // CEL expression
}