public interface NodeAuthoringAssistant {
    NodeTemplate generateTemplate(NodeGenerationRequest request);
    NodeDescriptor generateSchema(SchemaGenerationRequest request);
    String generateCode(CodeGenerationRequest request);
    ValidationResult validateGenerated(GeneratedNode node);
}