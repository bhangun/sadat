package tech.kayys.wayang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.AgentDefinition;
import tech.kayys.wayang.schema.ModelSpec;
import tech.kayys.wayang.schema.ToolDefinition;

import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schema Processor - Validates and processes agent definitions against JSON
 * schema
 */
@ApplicationScoped
public class SchemaProcessor {

    private static final Logger LOG = Logger.getLogger(SchemaProcessor.class);

    @Inject
    ObjectMapper objectMapper;

    private JsonSchema jsonSchema;
    private JsonNode schemaNode;

    @jakarta.annotation.PostConstruct
    void init() {
        try {
            // Load the schema from resources or configuration
            InputStream schemaStream = getClass().getResourceAsStream("/schema/agent-schema.json");
            if (schemaStream == null) {
                LOG.warn("Schema file not found, using default validation");
                return;
            }

            schemaNode = objectMapper.readTree(schemaStream);

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            jsonSchema = factory.getSchema(schemaNode);

            LOG.info("Schema loaded successfully");
        } catch (Exception e) {
            LOG.error("Failed to load schema", e);
        }
    }

    /**
     * Validate agent definition against schema
     */
    public Uni<ValidationResult> validateAgentDefinition(String jsonContent) {
        return Uni.createFrom().item(() -> {
            try {
                JsonNode agentNode = objectMapper.readTree(jsonContent);

                if (jsonSchema != null) {
                    Set<ValidationMessage> errors = jsonSchema.validate(agentNode);

                    if (!errors.isEmpty()) {
                        List<String> errorMessages = errors.stream()
                                .map(ValidationMessage::getMessage)
                                .collect(Collectors.toList());

                        return new ValidationResult(false, errorMessages);
                    }
                }

                // Additional custom validation
                List<String> customErrors = performCustomValidation(agentNode);

                if (!customErrors.isEmpty()) {
                    return new ValidationResult(false, customErrors);
                }

                return new ValidationResult(true, List.of());

            } catch (Exception e) {
                LOG.errorf(e, "Validation failed");
                return new ValidationResult(false, List.of("Invalid JSON: " + e.getMessage()));
            }
        });
    }

    /**
     * Parse and convert JSON to AgentDefinition
     */
    public Uni<AgentDefinition> parseAgentDefinition(String jsonContent) {
        return validateAgentDefinition(jsonContent)
                .chain(validation -> {
                    if (!validation.isValid()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Schema validation failed: " +
                                        String.join(", ", validation.getErrors())));
                    }

                    try {
                        JsonNode root = objectMapper.readTree(jsonContent);

                        // Handle both single agent and project format
                        JsonNode agentNode = root.has("agents") ? root.get("agents").get(0) : root;

                        AgentDefinition agent = objectMapper.treeToValue(agentNode, AgentDefinition.class);

                        return Uni.createFrom().item(agent);

                    } catch (Exception e) {
                        LOG.errorf(e, "Failed to parse agent definition");
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Failed to parse: " + e.getMessage()));
                    }
                });
    }

    /**
     * Convert AgentDefinition to JSON
     */
    public Uni<String> serializeAgentDefinition(AgentDefinition agent) {
        return Uni.createFrom().item(() -> {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(agent);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize agent", e);
            }
        });
    }

    /**
     * Create project wrapper with multiple agents
     */
    public Uni<String> createProjectJson(List<AgentDefinition> agents, ProjectMetadata metadata) {
        return Uni.createFrom().item(() -> {
            try {
                Map<String, Object> project = new HashMap<>();

                // Project metadata
                Map<String, Object> projectInfo = new HashMap<>();
                projectInfo.put("id", metadata.getId());
                projectInfo.put("name", metadata.getName());
                projectInfo.put("description", metadata.getDescription());

                Map<String, Object> projectMetadata = new HashMap<>();
                projectMetadata.put("createdAt", metadata.getCreatedAt());
                projectMetadata.put("updatedAt", metadata.getUpdatedAt());
                projectMetadata.put("version", metadata.getVersion());
                projectInfo.put("metadata", projectMetadata);

                project.put("project", projectInfo);
                project.put("agents", agents);

                // Shared resources
                project.put("sharedResources", extractSharedResources(agents));

                // Config
                Map<String, Object> config = new HashMap<>();
                config.put("version", "1.0.0");
                config.put("schemaVersion", "1.0.0");
                project.put("config", config);

                return objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(project);

            } catch (Exception e) {
                throw new RuntimeException("Failed to create project JSON", e);
            }
        });
    }

    /**
     * Custom validation rules
     */
    private List<String> performCustomValidation(JsonNode agentNode) {
        List<String> errors = new ArrayList<>();

        // Validate workflows
        if (agentNode.has("workflows")) {
            JsonNode workflows = agentNode.get("workflows");
            for (JsonNode workflow : workflows) {
                errors.addAll(validateWorkflow(workflow));
            }
        }

        // Validate LLM config
        if (agentNode.has("ModelSpec")) {
            errors.addAll(validateModelSpec(agentNode.get("ModelSpec")));
        }

        // Validate tools
        if (agentNode.has("tools")) {
            JsonNode tools = agentNode.get("tools");
            for (JsonNode tool : tools) {
                errors.addAll(validateTool(tool));
            }
        }

        return errors;
    }

    private List<String> validateWorkflow(JsonNode workflow) {
        List<String> errors = new ArrayList<>();

        if (!workflow.has("nodes") || workflow.get("nodes").size() == 0) {
            errors.add("Workflow must have at least one node");
            return errors;
        }

        // Check for start node
        boolean hasStart = false;
        JsonNode nodes = workflow.get("nodes");
        for (JsonNode node : nodes) {
            if ("start".equals(node.get("type").asText())) {
                hasStart = true;
                break;
            }
        }

        if (!hasStart) {
            errors.add("Workflow must have a START node");
        }

        // Validate edges reference valid nodes
        if (workflow.has("edges")) {
            Set<String> nodeIds = new HashSet<>();
            for (JsonNode node : nodes) {
                nodeIds.add(node.get("id").asText());
            }

            JsonNode edges = workflow.get("edges");
            for (JsonNode edge : edges) {
                String source = edge.get("source").asText();
                String target = edge.get("target").asText();

                if (!nodeIds.contains(source)) {
                    errors.add("Edge references non-existent source node: " + source);
                }
                if (!nodeIds.contains(target)) {
                    errors.add("Edge references non-existent target node: " + target);
                }
            }
        }

        return errors;
    }

    private List<String> validateModelSpec(JsonNode ModelSpec) {
        List<String> errors = new ArrayList<>();

        if (!ModelSpec.has("provider")) {
            errors.add("LLM config must have a provider");
        }

        if (!ModelSpec.has("model")) {
            errors.add("LLM config must have a model");
        }

        // Validate temperature range
        if (ModelSpec.has("parameters") &&
                ModelSpec.get("parameters").has("temperature")) {
            double temp = ModelSpec.get("parameters").get("temperature").asDouble();
            if (temp < 0 || temp > 2) {
                errors.add("Temperature must be between 0 and 2");
            }
        }

        return errors;
    }

    private List<String> validateTool(JsonNode tool) {
        List<String> errors = new ArrayList<>();

        if (!tool.has("name")) {
            errors.add("Tool must have a name");
        }

        if (!tool.has("type")) {
            errors.add("Tool must have a type");
        }

        if (tool.has("config") && tool.get("config").has("endpoint")) {
            String endpoint = tool.get("config").get("endpoint").asText();
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                errors.add("Tool endpoint must be a valid HTTP(S) URL");
            }
        }

        return errors;
    }

    private Map<String, Object> extractSharedResources(List<AgentDefinition> agents) {
        Map<String, Object> shared = new HashMap<>();

        // Extract common LLM configs
        List<ModelSpec> ModelSpecs = agents.stream()
                .map(AgentDefinition::getModelSpec)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        shared.put("ModelSpecs", ModelSpecs);

        // Extract common tools
        List<ToolDefinition> tools = agents.stream()
                .filter(a -> a.getTools() != null)
                .flatMap(a -> a.getTools().stream())
                .distinct()
                .collect(Collectors.toList());

        shared.put("tools", tools);

        return shared;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static class ProjectMetadata {
        private String id;
        private String name;
        private String description;
        private String createdAt;
        private String updatedAt;
        private String version;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
