@Value
@Builder
public class Binding {
    BindingType type;
    String source;  // variable name, node output, or literal value
    Optional<String> transformer;  // CEL expression for transformation
}