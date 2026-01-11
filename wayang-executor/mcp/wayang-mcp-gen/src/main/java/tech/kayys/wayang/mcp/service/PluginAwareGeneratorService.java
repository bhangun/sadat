package tech.kayys.wayang.mcp.service;

import tech.kayys.wayang.mcp.model.*;
import tech.kayys.wayang.mcp.config.PluginConfiguration;
import tech.kayys.wayang.mcp.plugin.*;
import tech.kayys.wayang.mcp.resource.McpGeneratorResource;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class PluginAwareGeneratorService {

    @Inject
    PluginManager pluginManager;

    @Inject
    PluginConfiguration pluginConfig;

    @Inject
    McpServerGeneratorService generatorService;

    void onStart(@Observes StartupEvent ev) {
        if (pluginConfig.enabled()) {
            pluginManager.initializePlugins();
            Log.info("Plugin system is enabled and initialized");
        } else {
            Log.info("Plugin system is disabled");
        }
    }

    public Uni<byte[]> generateMcpServerWithPlugins(InputStream inputFile, String filename,
            String packageName, String serverName,
            String baseUrl, boolean includeAuth,
            McpGeneratorResource.SpecificationType specType,
            String collectionName, Map<String, Object> pluginOptions) {
        return Uni.createFrom().item(() -> {
            if (!pluginConfig.enabled()) {
                // Fallback to original service
                return generatorService.generateMcpServer(inputFile, filename, packageName,
                        serverName, baseUrl, includeAuth, specType, collectionName).await().indefinitely();
            }

            try {
                PluginExecutionContext context = pluginManager.createExecutionContext();
                context.setConfiguration(pluginOptions != null ? pluginOptions : new HashMap<>());

                // Step 1: Process specification using plugins
                ApiSpecification apiSpec = processSpecificationWithPlugins(inputFile, filename,
                        specType, context);

                // Step 2: Validate with plugins
                validateWithPlugins(apiSpec, context);

                // Step 3: Generate model with plugins
                McpServerModel serverModel = generateModelWithPlugins(apiSpec, packageName,
                        serverName, baseUrl, includeAuth, context);

                // Step 4: Generate source files with plugins
                Map<String, String> sourceFiles = generateSourceFilesWithPlugins(serverModel, context);

                // Step 5: Create ZIP archive
                return createZipArchive(sourceFiles);

            } catch (Exception e) {
                Log.error("Plugin-aware generation failed, falling back to standard generation", e);
                // Fallback to original service
                return generatorService.generateMcpServer(inputFile, filename, packageName,
                        serverName, baseUrl, includeAuth, specType, collectionName).await().indefinitely();
            }
        });
    }

    private ApiSpecification processSpecificationWithPlugins(InputStream inputFile, String filename,
            McpGeneratorResource.SpecificationType specType,
            PluginExecutionContext context) throws Exception {

        // Try to find a plugin that can process this specification
        Optional<SpecificationProcessor> processor = pluginManager
                .getSpecificationProcessor(specType.name().toLowerCase());

        if (processor.isPresent()) {
            Log.debugf("Using plugin processor for spec type: %s", specType);
            return processor.get().processSpecification(inputFile, filename, context);
        } else {
            Log.debugf("No plugin processor found for spec type: %s, using built-in", specType);
            // Fallback to built-in processing
            throw new UnsupportedOperationException("Built-in fallback not implemented in this example");
        }
    }

    private void validateWithPlugins(ApiSpecification apiSpec, PluginExecutionContext context) throws Exception {
        List<ValidationPlugin> validators = pluginManager.getValidators("api-spec");

        for (ValidationPlugin validator : validators) {
            try {
                ValidationResult result = validator.validate(apiSpec, context);
                if (!result.isValid()) {
                    List<String> errors = result.getErrors().stream()
                            .map(ValidationResult.ValidationIssue::getMessage)
                            .toList();
                    throw new RuntimeException("Validation failed: " + String.join(", ", errors));
                }

                // Log warnings
                result.getWarnings().forEach(warning -> Log.warnf("Validation warning: %s", warning.getMessage()));

            } catch (Exception e) {
                Log.warnf("Validation plugin %s failed: %s", validator.getValidationType(), e.getMessage());
            }
        }
    }

    private McpServerModel generateModelWithPlugins(ApiSpecification apiSpec, String packageName,
            String serverName, String baseUrl, boolean includeAuth,
            PluginExecutionContext context) throws Exception {

        // Execute pre-processing plugins
        List<GeneratorPlugin> plugins = pluginManager.getAvailablePlugins();
        for (GeneratorPlugin plugin : plugins) {
            if (plugin.supports("model-generation")) {
                context.setAttribute("apiSpec", apiSpec);
                context.setAttribute("packageName", packageName);
                context.setAttribute("serverName", serverName);
                context.setAttribute("baseUrl", baseUrl);
                context.setAttribute("includeAuth", includeAuth);

                PluginResult result = plugin.execute(context);
                if (result.isSuccess()) {
                    Log.debugf("Plugin %s executed successfully for model generation", plugin.getId());
                    // Update context with plugin results
                    result.getData().forEach(context::setAttribute);
                }
            }
        }

        // Create model with potentially modified data
        return createServerModel(apiSpec,
                context.getAttribute("packageName", String.class),
                context.getAttribute("serverName", String.class),
                context.getAttribute("baseUrl", String.class),
                context.getAttribute("includeAuth", Boolean.class));
    }

    private Map<String, String> generateSourceFilesWithPlugins(McpServerModel serverModel,
            PluginExecutionContext context) throws Exception {
        Map<String, String> sourceFiles = new HashMap<>();

        // Get available template processors
        List<TemplateProcessor> processors = pluginManager.getTemplateProcessors("qute");

        if (processors.isEmpty()) {
            throw new RuntimeException("No template processors available");
        }

        TemplateProcessor processor = processors.get(0); // Use first available

        // Process each template
        Map<String, Object> templateData = createTemplateData(serverModel);

        // Main server class
        String mainTemplate = loadTemplate("mcp-server-main.java");
        String mainClassContent = processor.processTemplate(mainTemplate, templateData, context);
        sourceFiles.put("src/main/java/" + serverModel.getPackagePath() + "/" +
                serverModel.getServerClass() + ".java", mainClassContent);

        // Tool classes
        for (McpToolModel tool : serverModel.getTools()) {
            String toolTemplate = loadTemplate("mcp-tool.java");
            Map<String, Object> toolData = new HashMap<>(templateData);
            toolData.put("tool", tool);

            String toolClassContent = processor.processTemplate(toolTemplate, toolData, context);
            sourceFiles.put("src/main/java/" + serverModel.getPackagePath() + "/tools/" +
                    tool.getClassName() + ".java", toolClassContent);
        }

        // Configuration files
        String pomTemplate = loadTemplate("generated-pom.xml");
        String pomContent = processor.processTemplate(pomTemplate, templateData, context);
        sourceFiles.put("pom.xml", pomContent);

        return sourceFiles;
    }

    private Map<String, Object> createTemplateData(McpServerModel serverModel) {
        Map<String, Object> data = new HashMap<>();
        data.put("model", serverModel);
        data.put("timestamp", System.currentTimeMillis());
        data.put("generatorVersion", "1.0.0-plugin-enabled");
        return data;
    }

    private String loadTemplate(String templateName) {
        // This would load from resources - simplified for example
        return "Template content for " + templateName;
    }

    private McpServerModel createServerModel(ApiSpecification apiSpec, String packageName,
            String serverName, String baseUrl, boolean includeAuth) {
        // Convert ApiSpecification to McpServerModel
        // This is a simplified version - full implementation would mirror the original
        // service
        McpServerModel model = new McpServerModel();
        model.setPackageName(packageName);
        model.setServerName(serverName);
        model.setServerClass(serverName);
        model.setTitle(apiSpec.getTitle());
        model.setDescription(apiSpec.getDescription());
        model.setVersion(apiSpec.getVersion());
        model.setBaseUrl(baseUrl);
        model.setIncludeAuth(includeAuth);

        // Convert operations to tools
        List<McpToolModel> tools = apiSpec.getOperations().stream()
                .map(this::convertToMcpTool)
                .toList();
        model.setTools(tools);

        return model;
    }

    private McpToolModel convertToMcpTool(ApiOperation operation) {
        McpToolModel tool = new McpToolModel();
        tool.setName(operation.getOperationId() != null ? operation.getOperationId()
                : generateToolName(operation.getPath(), operation.getMethod()));
        tool.setDescription(operation.getDescription());
        tool.setPath(operation.getPath());
        tool.setMethod(operation.getMethod());
        tool.setOperationId(operation.getOperationId());
        tool.setSummary(operation.getSummary());

        // Convert parameters
        List<McpParameterModel> parameters = operation.getParameters().stream()
                .map(this::convertToMcpParameter)
                .toList();
        tool.setParameters(parameters);

        tool.setResponseTypes(operation.getResponseTypes());
        tool.setSecurityRequirements(operation.getSecurityRequirements());

        return tool;
    }

    private McpParameterModel convertToMcpParameter(ApiParameter apiParam) {
        McpParameterModel param = new McpParameterModel();
        param.setName(apiParam.getName());
        param.setDescription(apiParam.getDescription());
        param.setType(apiParam.getType());
        param.setRequired(apiParam.isRequired());
        param.setIn(apiParam.getIn());
        param.setExample(apiParam.getExample());
        param.setDefaultValue(apiParam.getDefaultValue());
        return param;
    }

    private String generateToolName(String path, String method) {
        String cleanPath = path.replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return method.toLowerCase() + "_" + cleanPath;
    }

    private byte[] createZipArchive(Map<String, String> files) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    public List<PluginInfo> getAvailablePlugins() {
        return pluginManager.getAvailablePlugins().stream()
                .map(plugin -> new PluginInfo(
                        plugin.getId(),
                        plugin.getName(),
                        plugin.getVersion(),
                        plugin.getDescription(),
                        plugin.getConfiguration()))
                .toList();
    }

    public static class PluginInfo {
        public final String id;
        public final String name;
        public final String version;
        public final String description;
        public final Map<String, Object> configuration;

        public PluginInfo(String id, String name, String version, String description,
                Map<String, Object> configuration) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
            this.configuration = configuration;
        }
    }
}
