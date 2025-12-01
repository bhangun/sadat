// Node Descriptor - Immutable Metadata
@Value
@Builder
public class NodeDescriptor {
    String id;
    String name;
    String version;
    SemanticVersion semanticVersion;
    NodeType type;
    List<InputPort> inputs;
    List<OutputPort> outputs;
    List<PropertyDescriptor> properties;
    Set<Capability> capabilities;
    List<String> requiredSecrets;
    Set<Permission> permissions;
    SandboxLevel sandboxLevel;
    ResourceProfile resourceProfile;
    Implementation implementation;
    String checksum;
    String signature;
    String publishedBy;
    Instant createdAt;
    NodeStatus status;
}
