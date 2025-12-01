
// Quarkus Generator
@ApplicationScoped
public class QuarkusServiceGenerator implements TargetGenerator {
    @Inject TemplateEngine templateEngine;
    
    @Override
    public GeneratedCode generate(
        WorkflowModel model,
        MinimizedDependencies deps,
        GenerationRequest request
    ) {
        List<GeneratedFile> files = new ArrayList<>();
        
        // Generate Main class
        files.add(generateMainClass(model));
        
        // Generate Node implementations
        for (NodeModel node : model.getNodes()) {
            files.add(generateNodeClass(node));
        }
        
        // Generate Orchestrator
        files.add(generateOrchestrator(model));
        
        // Generate application.properties
        files.add(generateApplicationProperties(model, request));
        
        // Generate pom.xml
        files.add(generatePomXml(deps));
        
        // Generate Dockerfile
        files.add(generateDockerfile());
        
        // Generate README
        files.add(generateReadme(model));
        
        return GeneratedCode.builder()
            .files(files)
            .entryPoint("io.generated.AgentApplication")
            .build();
    }
    
    private GeneratedFile generateMainClass(WorkflowModel model) {
        String template = """
            package io.generated;
            
            import io.quarkus.runtime.Quarkus;
            import io.quarkus.runtime.QuarkusApplication;
            import io.quarkus.runtime.annotations.QuarkusMain;
            import javax.inject.Inject;
            
            @QuarkusMain
            public class AgentApplication implements QuarkusApplication {
                
                @Inject
                AgentOrchestrator orchestrator;
                
                @Override
                public int run(String... args) throws Exception {
                    // Execute workflow
                    orchestrator.execute();
                    
                    Quarkus.waitForExit();
                    return 0;
                }
            }
            """;
        
        return GeneratedFile.builder()
            .path("src/main/java/io/generated/AgentApplication.java")
            .content(template)
            .type(FileType.JAVA_SOURCE)
            .build();
    }
    
    private GeneratedFile generateNodeClass(NodeModel node) {
        Map<String, Object> context = new HashMap<>();
        context.put("node", node);
        context.put("package", "io.generated.nodes");
        
        String content = templateEngine.process("node-template.java", context);
        
        return GeneratedFile.builder()
            .path("src/main/java/io/generated/nodes/" + node.getClassName() + ".java")
            .content(content)
            .type(FileType.JAVA_SOURCE)
            .build();
    }
    
    private GeneratedFile generateApplicationProperties(
        WorkflowModel model,
        GenerationRequest request
    ) {
        StringBuilder props = new StringBuilder();
        props.append("quarkus.application.name=").append(model.getName()).append("\n");
        props.append("quarkus.log.level=INFO\n");
        
        if (request.getOptions().isIncludeObservability()) {
            props.append("quarkus.otel.enabled=true\n");
            props.append("quarkus.otel.exporter.otlp.endpoint=http://localhost:4317\n");
        }
        
        // Add custom configuration
        for (Map.Entry<String, String> entry : request.getOptions().getConfiguration().entrySet()) {
            props.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        
        return GeneratedFile.builder()
            .path("src/main/resources/application.properties")
            .content(props.toString())
            .type(FileType.PROPERTIES)
            .build();
    }
    
    private GeneratedFile generatePomXml(MinimizedDependencies deps) {
        String template = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>io.generated</groupId>
                <artifactId>standalone-agent</artifactId>
                <version>1.0.0</version>
                
                <properties>
                    <quarkus.version>3.6.0</quarkus.version>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                </properties>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-bom</artifactId>
                            <version>${quarkus.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                
                <dependencies>
                    <!-- Quarkus Core -->
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-arc</artifactId>
                    </dependency>
                    
                    <!-- Add minimized dependencies -->
                    %s
                </dependencies>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-maven-plugin</artifactId>
                            <version>${quarkus.version}</version>
                            <executions>
                                <execution>
                                    <goals>
                                        <goal>build</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        
        String dependenciesXml = deps.getDependencies().stream()
            .map(this::toDependencyXml)
            .collect(Collectors.joining("\n"));
        
        String content = String.format(template, dependenciesXml);
        
        return GeneratedFile.builder()
            .path("pom.xml")
            .content(content)
            .type(FileType.YAML_CONFIG)
            .build();
    }
}
