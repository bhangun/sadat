package tech.kayys.wayang.codegen.optimizer;

import tech.kayys.wayang.codegen.model.DependencySpec;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimizes dependency tree to minimize size.
 * Performs "tree-shaking" to remove unused dependencies.
 */
public class DependencyOptimizer {
    
    /**
     * Remove transitive dependencies that are not actually used.
     */
    public Set<DependencySpec> optimize(Set<DependencySpec> dependencies) {
        // Build dependency graph
        Map<String, Set<String>> graph = buildDependencyGraph(dependencies);
        
        // Find actually used dependencies
        Set<String> used = findUsedDependencies(graph);
        
        // Filter to only used dependencies
        return dependencies.stream()
            .filter(dep -> used.contains(dep.getArtifactId()))
            .collect(Collectors.toSet());
    }
    
    /**
     * Suggest lighter alternatives for dependencies.
     */
    public Set<DependencySpec> suggestAlternatives(Set<DependencySpec> dependencies) {
        Set<DependencySpec> optimized = new HashSet<>();
        
        for (DependencySpec dep : dependencies) {
            DependencySpec alternative = findLighterAlternative(dep);
            optimized.add(alternative != null ? alternative : dep);
        }
        
        return optimized;
    }
    
    /**
     * Calculate final artifact size.
     */
    public long calculateSize(Set<DependencySpec> dependencies) {
        return dependencies.stream()
            .mapToLong(this::getDependencySize)
            .sum();
    }
    
    private long getDependencySize(DependencySpec dep) {
        // Look up actual size from repository metadata
        // Or use estimates based on artifact type
        
        if (dep.getArtifactId().contains("minimal")) {
            return 2_000_000; // 2MB
        } else if (dep.getArtifactId().contains("llm")) {
            return 5_000_000; // 5MB
        } else if (dep.getArtifactId().contains("rag")) {
            return 3_000_000; // 3MB
        }
        
        return 1_000_000; // 1MB default
    }
    
    private DependencySpec findLighterAlternative(DependencySpec dep) {
        // Example: Use lighter HTTP client if only basic features needed
        if (dep.getArtifactId().equals("okhttp") && !requiresAdvancedFeatures()) {
            return DependencySpec.builder()
                .groupId("com.squareup.okhttp3")
                .artifactId("okhttp-lightweight")
                .version(dep.getVersion())
                .build();
        }
        
        return null;
    }
    
    private boolean requiresAdvancedFeatures() {
        // Analyze if advanced features are actually used
        return false;
    }
}