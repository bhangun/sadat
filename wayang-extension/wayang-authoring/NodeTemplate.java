
@Value
@Builder
public class NodeTemplate {
    String id;
    String name;
    String category;
    String description;
    NodeDescriptor descriptor;
    String codeTemplate;
    List<String> dependencies;
}