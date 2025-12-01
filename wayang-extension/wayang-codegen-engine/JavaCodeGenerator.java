package tech.kayys.wayang.codegen.synthesizer;

import tech.kayys.wayang.core.workflow.WorkflowDefinition;
import tech.kayys.wayang.core.workflow.NodeInstance;
import tech.kayys.wayang.codegen.analyzer.AnalysisResult;
import tech.kayys.wayang.codegen.template.TemplateEngine;

import java.util.*;

/**
 * Generates Java code for standalone agent.
 * Only includes code for nodes actually used in workflow.
 */
public class JavaCodeGenerator {
    
    private final TemplateEngine templateEngine;
    
    public JavaCodeGenerator() {
        this.templateEngine = new TemplateEngine();
    }
    
    public GeneratedCode generate(
        WorkflowDefinition workflow,
        AnalysisResult analysis
    ) {
        List<GeneratedFile> files = new ArrayList<>();
        
        // Generate Main class
        files.add(generateMainClass(workflow, analysis));
        
        // Generate only used node implementations
        for (String nodeType : analysis.getUsedNodeTypes()) {
            files.add(generateNodeImplementation(nodeType, workflow));
        }
        
        // Generate minimal orchestrator
        files.add(generateMinimalOrchestrator(workflow));
        
        // Generate pom.xml with ONLY required dependencies
        files.add(generatePom(analysis));
        
        // Generate configuration
        files.add(generateConfiguration(workflow, analysis));
        
        // Generate Dockerfile
        files.add(generateDockerfile(analysis));
        
        return GeneratedCode.builder()
            .files(files)
            .entryPoint("tech.kayys.generated.StandaloneAgent")
            .build();
    }
    
    private GeneratedFile generateMainClass(
        WorkflowDefinition workflow,
        AnalysisResult analysis
    ) {
        Map<String, Object> context = new HashMap<>();
        context.put("workflowName", workflow.getMetadata().getName());
        context.put("nodes", workflow.getNodes());
        context.put("capabilities", analysis.getRequiredCapabilities());
        
        String content = templateEngine.process("java/main-class.ftl", context);
        
        return GeneratedFile.builder()
            .path("src/main/java/tech/kayys/generated/StandaloneAgent.java")
            .content(content)
            .type(FileType.JAVA_SOURCE)
            .build();
    }
    
    private GeneratedFile generatePom(AnalysisResult analysis) {
        Map<String, Object> context = new HashMap<>();
        context.put("dependencies", analysis.getDependencies());
        context.put("estimatedSize", analysis.getEstimatedSize());
        
        String content = templateEngine.process("java/pom.ftl", context);
        
        return GeneratedFile.builder()
            .path("pom.xml")
            .content(content)
            .type(FileType.XML)
            .build();
    }
    
    private GeneratedFile generateNodeImplementation(
        String nodeType,
        WorkflowDefinition workflow
    ) {
        // Find all instances of this node type
        List<NodeInstance> instances = workflow.getNodes().stream()
            .filter(n -> n.getNodeType().equals(nodeType))
            .collect(Collectors.toList());
        
        Map<String, Object> context = new HashMap<>();
        context.put("nodeType", nodeType);
        context.put("instances", instances);
        
        String content = templateEngine.process(
            "java/node-" + nodeType.toLowerCase() + ".ftl",
            context
        );
        
        return GeneratedFile.builder()
            .path("src/main/java/tech/kayys/generated/nodes/" + 
                  nodeType + "Implementation.java")
            .content(content)
            .type(FileType.JAVA_SOURCE)
            .build();
    }
}