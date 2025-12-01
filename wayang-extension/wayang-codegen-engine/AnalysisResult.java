@Value
@Builder
class AnalysisResult {
    Set<String> usedNodeTypes;
    Set<String> requiredCapabilities;
    Set<DependencySpec> dependencies;
    long estimatedSize;
}