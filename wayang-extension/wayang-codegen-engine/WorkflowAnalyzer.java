package tech.kayys.wayang.codegen.analyzer;

import tech.kayys.wayang.core.workflow.WorkflowDefinition;
import tech.kayys.wayang.core.workflow.NodeInstance;
import tech.kayys.wayang.codegen.model.DependencySpec;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes workflow to determine minimal required dependencies.
 */
public class WorkflowAnalyzer {
    
    private final NodeDependencyAnalyzer nodeDependencyAnalyzer;
    private final CapabilityAnalyzer capabilityAnalyzer;
    
    public WorkflowAnalyzer() {
        this.nodeDependencyAnalyzer = new NodeDependencyAnalyzer();
        this.capabilityAnalyzer = new CapabilityAnalyzer();
    }
    
    /**
     * Analyze workflow and return minimal dependencies.
     */
    public AnalysisResult analyze(WorkflowDefinition workflow) {
        Set<String> usedNodeTypes = new HashSet<>();
        Set<String> requiredCapabilities = new HashSet<>();
        Set<DependencySpec> dependencies = new HashSet<>();
        
        // Analyze each node
        for (NodeInstance node : workflow.getNodes()) {
            usedNodeTypes.add(node.getNodeType());
            
            // Get node-specific dependencies
            Set<DependencySpec> nodeDeps = nodeDependencyAnalyzer.analyze(node);
            dependencies.addAll(nodeDeps);
            
            // Get capabilities
            Set<String> caps = capabilityAnalyzer.analyze(node);
            requiredCapabilities.addAll(caps);
        }
        
        // Add base dependencies (always required)
        dependencies.add(DependencySpec.builder()
            .groupId("tech.kayys.wayang")
            .artifactId("wayang-runtime-minimal")
            .version("${wayang.version}")
            .scope("compile")
            .build());
        
        // Add capability-based dependencies
        if (requiredCapabilities.contains("LLM_ACCESS")) {
            dependencies.add(DependencySpec.builder()
                .groupId("tech.kayys.wayang")
                .artifactId("wayang-runtime-llm")
                .version("${wayang.version}")
                .build());
        }
        
        if (requiredCapabilities.contains("RAG_QUERY")) {
            dependencies.add(DependencySpec.builder()
                .groupId("tech.kayys.wayang")
                .artifactId("wayang-runtime-rag")
                .version("${wayang.version}")
                .build());
        }
        
        if (requiredCapabilities.contains("TOOL_EXECUTION")) {
            dependencies.add(DependencySpec.builder()
                .groupId("tech.kayys.wayang")
                .artifactId("wayang-runtime-tools")
                .version("${wayang.version}")
                .build());
        }
        
        // Add node-type specific dependencies
        for (String nodeType : usedNodeTypes) {
            dependencies.add(DependencySpec.builder()
                .groupId("tech.kayys.wayang")
                .artifactId("wayang-node-" + nodeType.toLowerCase())
                .version("${wayang.version}")
                .build());
        }
        
        return AnalysisResult.builder()
            .usedNodeTypes(usedNodeTypes)
            .requiredCapabilities(requiredCapabilities)
            .dependencies(dependencies)
            .estimatedSize(estimateSize(dependencies))
            .build();
    }
    
    private long estimateSize(Set<DependencySpec> dependencies) {
        // Estimate final JAR size based on dependencies
        long baseSize = 5_000_000; // 5MB for base runtime
        long perNodeSize = 500_000; // 500KB per node type
        long perCapabilitySize = 2_000_000; // 2MB per capability
        
        return baseSize + 
               (dependencies.size() * perNodeSize);
    }
}