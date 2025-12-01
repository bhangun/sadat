@Value
@Builder
public class SchemaGenerationRequest {
    String nodeType;
    String description;
    List<PropertySpec> properties;
    List<PortSpec> inputs;
    List<PortSpec> outputs;
}
