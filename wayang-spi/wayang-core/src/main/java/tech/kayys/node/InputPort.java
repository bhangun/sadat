@Value
@Builder
public class InputPort {
    String name;
    DataType type;
    boolean required;
    Object defaultValue;
    JsonSchema schema;
}
