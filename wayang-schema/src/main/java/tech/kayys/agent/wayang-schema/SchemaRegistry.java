
public interface SchemaRegistry {
    void registerSchema(NodeDescriptor descriptor);
    Optional<NodeDescriptor> getDescriptor(String descriptorId);
    Optional<NodeDescriptor> getDescriptor(String descriptorId, String version);
    List<NodeDescriptor> listDescriptors(SchemaQuery query);
    void updateSchema(String descriptorId, NodeDescriptor descriptor);
    void deprecateSchema(String descriptorId);
    ValidationResult validateSchema(NodeDescriptor descriptor);
}