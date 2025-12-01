@Value
@Builder
public class NodeInstance {
    String nodeId;
    String nodeType;
    String descriptorId;
    String descriptorVersion;
    Map<String, Object> config;
    Map<String, Binding> inputBindings;
    Map<String, Binding> outputBindings;
    List<String> capabilities;
    Position position;  // For UI
}