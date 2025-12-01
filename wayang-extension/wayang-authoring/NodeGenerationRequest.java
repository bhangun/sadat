
@Value
@Builder
public class NodeGenerationRequest {
    String purpose;
    String category;
    JsonSchema inputSchema;
    JsonSchema outputSchema;
    Set<Capability> capabilities;
    Map<String, Object> hints;
}
