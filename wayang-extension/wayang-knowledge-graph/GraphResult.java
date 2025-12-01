@Value
@Builder
public class GraphResult {
    List<Entity> entities;
    List<Relation> relations;
    List<Path> paths;
}