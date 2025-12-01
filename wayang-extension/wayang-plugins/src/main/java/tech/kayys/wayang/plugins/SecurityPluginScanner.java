@ApplicationScoped
public class SecurityPluginScanner {
    @Inject DependencyAnalyzer dependencyAnalyzer;
    @Inject VulnerabilityDatabase vulnerabilityDB;
    
    public ScanResult scan(String artifactId) {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        
        // Analyze dependencies
        List<Dependency> dependencies = dependencyAnalyzer.analyze(artifactId);
        
        for (Dependency dep : dependencies) {
            List<Vulnerability> depVulns = vulnerabilityDB.check(
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getVersion()
            );
            vulnerabilities.addAll(depVulns);
        }
        
        return ScanResult.builder()
            .artifactId(artifactId)
            .vulnerabilities(vulnerabilities)
            .scannedAt(Instant.now())
            .build();
    }
}
