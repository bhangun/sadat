
public interface StandaloneAgentGenerator {
    GeneratedArtifact generate(GenerationRequest request);
    List<GenerationTarget> getSupportedTargets();
    ValidationResult validateRequest(GenerationRequest request);
}
