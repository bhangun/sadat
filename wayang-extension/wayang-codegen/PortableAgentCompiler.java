
@ApplicationScoped
public class PortableAgentCompiler implements StandaloneAgentGenerator {
    @Inject ModelExtractor modelExtractor;
    @Inject DependencyMinimizer dependencyMinimizer;
    @Inject RuntimePackager runtimePackager;
    @Inject ArtifactPublisher artifactPublisher;
    
    @Override
    public GeneratedArtifact generate(GenerationRequest request) {
        // Extract workflow model
        WorkflowModel model = modelExtractor.extract(request.getWorkflowId());
        
        // Minimize dependencies
        MinimizedDependencies deps = dependencyMinimizer.minimize(model);
        
        // Select generator based on target
        TargetGenerator generator = selectGenerator(request.getTarget());
        
        // Generate code
        GeneratedCode code = generator.generate(model, deps, request);
        
        // Package artifact
        PackagedArtifact artifact = runtimePackager.packageArtifact(
            code,
            request.getTarget()
        );
        
        // Publish if requested
        if (request.isPublish()) {
            artifactPublisher.publish(artifact, request.getPublishConfig());
        }
        
        return GeneratedArtifact.builder()
            .artifactId(artifact.getId())
            .target(request.getTarget())
            .files(code.getFiles())
            .buildInstructions(artifact.getBuildInstructions())
            .deploymentInstructions(artifact.getDeploymentInstructions())
            .build();
    }
    
    private TargetGenerator selectGenerator(GenerationTarget target) {
        switch (target) {
            case QUARKUS_JAR:
                return quarkusGenerator;
            case NATIVE_IMAGE:
                return nativeImageGenerator;
            case DOCKER:
                return dockerGenerator;
            case PYTHON_PACKAGE:
                return pythonGenerator;
            case WASM_MODULE:
                return wasmGenerator;
            case FLUTTER_PLUGIN:
                return flutterGenerator;
            default:
                throw new UnsupportedTargetException(target);
        }
    }
}
