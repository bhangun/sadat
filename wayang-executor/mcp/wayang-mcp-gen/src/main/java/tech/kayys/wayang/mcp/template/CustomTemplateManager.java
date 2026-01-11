package tech.kayys.wayang.mcp.template;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CustomTemplateManager {

    private final Map<String, CustomTemplate> templates = new ConcurrentHashMap<>();
    private final Map<String, TemplateSet> templateSets = new ConcurrentHashMap<>();
    private final Path templatesDirectory;

    public CustomTemplateManager() {
        this.templatesDirectory = Paths.get(System.getProperty("user.home"), ".mcp-generator", "templates");
        initializeTemplateDirectory();
        loadBuiltInTemplates();
        loadUserTemplates();
    }

    private void initializeTemplateDirectory() {
        try {
            if (!Files.exists(templatesDirectory)) {
                Files.createDirectories(templatesDirectory);
                Log.infof("Created templates directory: %s", templatesDirectory);
            }
        } catch (IOException e) {
            Log.error("Failed to create templates directory", e);
        }
    }

    private void loadBuiltInTemplates() {
        Log.info("Loading built-in templates...");

        // Standard Quarkus templates
        TemplateSet standardSet = new TemplateSet(
                "standard",
                "Standard Quarkus MCP Server",
                "Default templates for generating Quarkus-based MCP servers",
                "1.0.0");

        standardSet.addTemplate(new CustomTemplate(
                "mcp-server-main",
                "Main MCP Server Class",
                "java",
                loadBuiltInTemplateContent("mcp-server-main.java"),
                Map.of("framework", "quarkus", "type", "main-class")));

        standardSet.addTemplate(new CustomTemplate(
                "mcp-tool",
                "MCP Tool Implementation",
                "java",
                loadBuiltInTemplateContent("mcp-tool.java"),
                Map.of("framework", "quarkus", "type", "tool-class")));

        standardSet.addTemplate(new CustomTemplate(
                "pom-xml",
                "Maven POM Configuration",
                "xml",
                loadBuiltInTemplateContent("generated-pom.xml"),
                Map.of("build-tool", "maven", "type", "config")));

        templateSets.put("standard", standardSet);

        // Spring Boot templates (example)
        TemplateSet springBootSet = new TemplateSet(
                "spring-boot",
                "Spring Boot MCP Server",
                "Templates for Spring Boot-based MCP servers",
                "1.0.0");

        springBootSet.addTemplate(new CustomTemplate(
                "spring-mcp-server",
                "Spring Boot MCP Server",
                "java",
                generateSpringBootMainTemplate(),
                Map.of("framework", "spring-boot", "type", "main-class")));

        springBootSet.addTemplate(new CustomTemplate(
                "spring-tool",
                "Spring Boot MCP Tool",
                "java",
                generateSpringBootToolTemplate(),
                Map.of("framework", "spring-boot", "type", "tool-class")));

        templateSets.put("spring-boot", springBootSet);

        Log.infof("Loaded %d built-in template sets", templateSets.size());
    }

    private void loadUserTemplates() {
        Log.infof("Loading user templates from: %s", templatesDirectory);

        try {
            if (Files.exists(templatesDirectory)) {
                Files.walk(templatesDirectory)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".template"))
                        .forEach(this::loadUserTemplate);
            }
        } catch (IOException e) {
            Log.error("Failed to load user templates", e);
        }
    }

    private void loadUserTemplate(Path templateFile) {
        try {
            String content = Files.readString(templateFile);
            CustomTemplateMetadata metadata = parseTemplateMetadata(content);

            CustomTemplate template = new CustomTemplate(
                    metadata.id,
                    metadata.name,
                    metadata.fileType,
                    extractTemplateContent(content),
                    metadata.properties);

            templates.put(template.getId(), template);
            Log.debugf("Loaded user template: %s", template.getId());

        } catch (Exception e) {
            Log.error("Failed to load template file: " + templateFile, e);
        }
    }

    public List<TemplateSet> getAvailableTemplateSets() {
        return new ArrayList<>(templateSets.values());
    }

    public Optional<TemplateSet> getTemplateSet(String setId) {
        return Optional.ofNullable(templateSets.get(setId));
    }

    public List<CustomTemplate> getTemplatesForFramework(String framework) {
        return templates.values().stream()
                .filter(template -> framework.equals(template.getProperty("framework")))
                .toList();
    }

    public Optional<CustomTemplate> getTemplate(String templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }

    public void saveUserTemplate(CustomTemplate template) throws IOException {
        Path templateFile = templatesDirectory.resolve(template.getId() + ".template");

        String templateContent = formatTemplateFile(template);
        Files.writeString(templateFile, templateContent);

        templates.put(template.getId(), template);
        Log.infof("Saved user template: %s", template.getId());
    }

    public void deleteUserTemplate(String templateId) throws IOException {
        Path templateFile = templatesDirectory.resolve(templateId + ".template");

        if (Files.exists(templateFile)) {
            Files.delete(templateFile);
            templates.remove(templateId);
            Log.infof("Deleted user template: %s", templateId);
        }
    }

    public TemplateRenderResult renderTemplate(String templateId, Map<String, Object> data) {
        Optional<CustomTemplate> template = getTemplate(templateId);

        if (template.isEmpty()) {
            return TemplateRenderResult.error("Template not found: " + templateId);
        }

        try {
            String rendered = renderTemplateContent(template.get().getContent(), data);
            return TemplateRenderResult.success(rendered);

        } catch (Exception e) {
            Log.error("Template rendering failed", e);
            return TemplateRenderResult.error("Rendering failed: " + e.getMessage());
        }
    }

    private String loadBuiltInTemplateContent(String templateName) {
        // In a real implementation, this would load from resources
        return "Built-in template content for: " + templateName;
    }

    private String generateSpringBootMainTemplate() {
        return """
                package {model.packageName};

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                import org.springframework.web.bind.annotation.*;
                import com.fasterxml.jackson.databind.JsonNode;

                /**
                 * {model.title}
                 * Spring Boot MCP Server
                 * Version: {model.version}
                 */
                @SpringBootApplication
                @RestController
                public class {model.serverClass} {

                    public static void main(String[] args) {
                        SpringApplication.run({model.serverClass}.class, args);
                    }

                    @PostMapping("/mcp")
                    public JsonNode handleMcpRequest(@RequestBody JsonNode request) {
                        // MCP request handling logic
                        return null;
                    }

                    @GetMapping("/health")
                    public Map<String, Object> health() {
                        return Map.of(
                            "status", "UP",
                            "server", "{model.title}",
                            "version", "{model.version}"
                        );
                    }
                }
                """;
    }

    private String generateSpringBootToolTemplate() {
        return """
                package {model.packageName}.tools;

                import org.springframework.stereotype.Component;
                import org.springframework.web.client.RestTemplate;
                import com.fasterxml.jackson.databind.JsonNode;

                /**
                 * Spring Boot MCP Tool: {tool.name}
                 * {tool.description}
                 */
                @Component
                public class {tool.className} {

                    private final RestTemplate restTemplate = new RestTemplate();

                    public JsonNode execute(JsonNode arguments) {
                        // Tool execution logic
                        return null;
                    }
                }
                """;
    }

    private CustomTemplateMetadata parseTemplateMetadata(String content) {
        // Parse template metadata from content
        // Format: ---metadata--- at the top of the file
        CustomTemplateMetadata metadata = new CustomTemplateMetadata();

        if (content.startsWith("---")) {
            int endIndex = content.indexOf("---", 3);
            if (endIndex != -1) {
                String metadataSection = content.substring(3, endIndex);
                parseMetadataSection(metadataSection, metadata);
            }
        }

        return metadata;
    }

    private void parseMetadataSection(String metadataSection, CustomTemplateMetadata metadata) {
        String[] lines = metadataSection.split("\n");
        for (String line : lines) {
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "id" -> metadata.id = value;
                    case "name" -> metadata.name = value;
                    case "fileType" -> metadata.fileType = value;
                    case "framework" -> metadata.properties.put("framework", value);
                    case "type" -> metadata.properties.put("type", value);
                    default -> metadata.properties.put(key, value);
                }
            }
        }
    }

    private String extractTemplateContent(String content) {
        if (content.startsWith("---")) {
            int endIndex = content.indexOf("---", 3);
            if (endIndex != -1) {
                return content.substring(endIndex + 3).trim();
            }
        }
        return content;
    }

    private String formatTemplateFile(CustomTemplate template) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("id: ").append(template.getId()).append("\n");
        sb.append("name: ").append(template.getName()).append("\n");
        sb.append("fileType: ").append(template.getFileType()).append("\n");

        template.getProperties().forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));

        sb.append("---\n");
        sb.append(template.getContent());

        return sb.toString();
    }

    private String renderTemplateContent(String content, Map<String, Object> data) {
        // Simple template rendering - in production, use a proper template engine
        String rendered = content;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            rendered = rendered.replace(placeholder, value);
        }

        return rendered;
    }

    private static class CustomTemplateMetadata {
        public String id = "";
        public String name = "";
        public String fileType = "java";
        public Map<String, Object> properties = new HashMap<>();
    }
}
