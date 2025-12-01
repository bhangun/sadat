@Value
@Builder
public class PluginDescriptor {
    String id;
    String name;
    String version;
    String description;
    String author;
    List<NodeDescriptor> nodeDescriptors;
    List<String> dependencies;
    Set<Permission> requiredPermissions;
    String signature;
    Map<String, Object> metadata;
}