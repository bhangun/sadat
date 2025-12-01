@Value
@Builder
public class CodeGenerationRequest {
    String language;
    String nodeType;
    String description;
    Map<String, DataType> inputs;
    Map<String, DataType> outputs;
    String logicDescription;
    List<String> examples;
}
