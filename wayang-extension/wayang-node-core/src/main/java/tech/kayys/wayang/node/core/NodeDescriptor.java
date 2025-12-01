
/**
 * Node descriptor - immutable metadata about a node type
 */
@Immutable
public final class NodeDescriptor {
    private final String id;
    private final String name;
    private final String version;
    private final List<InputDescriptor> inputs;
    private final List<OutputDescriptor> outputs;
    private final List<PropertyDescriptor> properties;
    private final Set<String> capabilities;
    private final String implementationArtifact;
    private final SandboxLevel sandboxLevel;
    private final ResourceProfile resourceProfile;
    
    // Constructor and getters...
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        // Builder implementation...
    }
}