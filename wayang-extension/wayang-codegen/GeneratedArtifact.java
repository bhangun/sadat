@Value
@Builder
public class GeneratedArtifact {
    String artifactId;
    GenerationTarget target;
    List<GeneratedFile> files;
    String buildInstructions;
    String deploymentInstructions;
}