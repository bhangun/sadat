@Value
@Builder
public class Entity {
    String id;
    String name;
    String type;
    Map<String, Object> properties;
    float[] embedding;
}