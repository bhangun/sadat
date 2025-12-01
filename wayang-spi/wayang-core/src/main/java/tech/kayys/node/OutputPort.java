@Value
@Builder
public class OutputPort {
    String name;
    DataType type;
    JsonSchema schema;
}