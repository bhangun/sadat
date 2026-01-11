package tech.kayys.wayang.mcp.resource;

import tech.kayys.wayang.mcp.config.McpServerConfig;
import tech.kayys.wayang.mcp.service.PluginAwareGeneratorService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Path("/api/mcp-generator-v2")
@Produces(MediaType.APPLICATION_JSON)
public class McpGeneratorResourceEnhanced {

    @Inject
    PluginAwareGeneratorService pluginService;

    @Inject
    McpServerConfig config;

    public static class EnhancedFileUpload {
        @RestForm("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        @NotNull
        public InputStream file;

        @RestForm("filename")
        @PartType(MediaType.TEXT_PLAIN)
        public String filename;

        @RestForm("packageName")
        @PartType(MediaType.TEXT_PLAIN)
        public String packageName;

        @RestForm("serverName")
        @PartType(MediaType.TEXT_PLAIN)
        public String serverName;

        @RestForm("baseUrl")
        @PartType(MediaType.TEXT_PLAIN)
        public String baseUrl;

        @RestForm("includeAuth")
        @PartType(MediaType.TEXT_PLAIN)
        public String includeAuth = "false";

        @RestForm("specType")
        @PartType(MediaType.TEXT_PLAIN)
        public String specType = "auto";

        @RestForm("collectionName")
        @PartType(MediaType.TEXT_PLAIN)
        public String collectionName;

        @RestForm("enablePlugins")
        @PartType(MediaType.TEXT_PLAIN)
        public String enablePlugins = "true";

        @RestForm("pluginOptions")
        @PartType(MediaType.TEXT_PLAIN)
        public String pluginOptionsJson;

        public String getPackageNameOrDefault(String defaultValue) {
            return packageName != null && !packageName.trim().isEmpty() ? packageName : defaultValue;
        }

        public String getServerNameOrDefault(String defaultValue) {
            return serverName != null && !serverName.trim().isEmpty() ? serverName : defaultValue;
        }

        public String getBaseUrlOrDefault(String defaultValue) {
            return baseUrl != null && !baseUrl.trim().isEmpty() ? baseUrl : defaultValue;
        }

        public boolean isIncludeAuth() {
            return "true".equalsIgnoreCase(includeAuth);
        }

        public boolean isEnablePlugins() {
            return "true".equalsIgnoreCase(enablePlugins);
        }

        public McpGeneratorResource.SpecificationType getSpecType() {
            if (specType == null)
                return McpGeneratorResource.SpecificationType.AUTO;
            return switch (specType.toLowerCase()) {
                case "openapi" -> McpGeneratorResource.SpecificationType.OPENAPI;
                case "postman" -> McpGeneratorResource.SpecificationType.POSTMAN;
                case "insomnia" -> McpGeneratorResource.SpecificationType.OPENAPI; // Will be detected by plugin
                default -> McpGeneratorResource.SpecificationType.AUTO;
            };
        }

        public String getCollectionNameOrDefault(String defaultValue) {
            return collectionName != null && !collectionName.trim().isEmpty() ? collectionName : defaultValue;
        }

        public Map<String, Object> getPluginOptions() {
            if (pluginOptionsJson == null || pluginOptionsJson.trim().isEmpty()) {
                return new HashMap<>();
            }

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(pluginOptionsJson, Map.class);
            } catch (Exception e) {
                Log.warnf("Failed to parse plugin options JSON: %s", e.getMessage());
                return new HashMap<>();
            }
        }
    }

    @POST
    @Path("/generate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> generateMcpServerWithPlugins(@MultipartForm EnhancedFileUpload upload) {
        Log.infof("Starting enhanced MCP server generation with plugins for file: %s", upload.filename);

        if (upload.isEnablePlugins()) {
            return pluginService.generateMcpServerWithPlugins(
                    upload.file,
                    upload.filename,
                    upload.getPackageNameOrDefault(config.defaultPackage()),
                    upload.getServerNameOrDefault(config.defaultServerName()),
                    upload.getBaseUrlOrDefault(config.defaultBaseUrl()),
                    upload.isIncludeAuth(),
                    upload.getSpecType(),
                    upload.getCollectionNameOrDefault("Generated Collection"),
                    upload.getPluginOptions()).map(zipBytes -> {
                        String filename = upload.getServerNameOrDefault(config.defaultServerName()).toLowerCase()
                                + "-mcp-server-v2.zip";
                        Log.infof("Successfully generated enhanced MCP server ZIP: %s (%d bytes)", filename,
                                zipBytes.length);

                        return Response.ok(zipBytes)
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .header("Content-Type", "application/zip")
                                .header("Content-Length", String.valueOf(zipBytes.length))
                                .header("X-Generator-Version", "2.0-plugin-enabled")
                                .build();
                    })
                    .onFailure().recoverWithItem(throwable -> {
                        Log.error("Enhanced generation failed", throwable);

                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(new ErrorResponse("Enhanced generation failed: " + throwable.getMessage()))
                                .type(MediaType.APPLICATION_JSON)
                                .build();
                    });
        } else {
            Log.infof("Plugins disabled, falling back to standard generation");
            // Fallback to standard generation (would need to inject the original service)
            return Uni.createFrom().item(
                    Response.status(Response.Status.NOT_IMPLEMENTED)
                            .entity(new ErrorResponse("Standard generation fallback not implemented in this example"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
    }

    @GET
    @Path("/capabilities")
    public Response getCapabilities() {
        return Response.ok(new CapabilitiesResponse(
                pluginService.getAvailablePlugins(),
                config.supportedVersions(),
                config.supportedSpecTypes(),
                config.maxFileSize())).build();
    }

    @POST
    @Path("/validate-with-plugins")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> validateWithPlugins(@MultipartForm EnhancedFileUpload upload) {
        Log.infof("Validating specification with plugins: %s", upload.filename);

        return Uni.createFrom().item(() -> {
            try {
                // This would use plugin-aware validation
                // Simplified for this example
                return Response.ok(new ValidationResponse(
                        true,
                        "Plugin-aware validation completed",
                        java.util.List.of(),
                        java.util.List.of("Using enhanced plugin validation"))).build();

            } catch (Exception e) {
                Log.error("Plugin validation failed", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Validation failed: " + e.getMessage()))
                        .build();
            }
        });
    }

    // Response DTOs
    public static class ErrorResponse {
        public final String error;
        public final long timestamp;
        public final String version = "2.0-plugin-enabled";

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class CapabilitiesResponse {
        public final java.util.List<PluginAwareGeneratorService.PluginInfo> availablePlugins;
        public final String[] supportedOpenApiVersions;
        public final String[] supportedSpecTypes;
        public final long maxFileSize;
        public final String version = "2.0-plugin-enabled";

        public CapabilitiesResponse(java.util.List<PluginAwareGeneratorService.PluginInfo> availablePlugins,
                String[] supportedOpenApiVersions, String[] supportedSpecTypes,
                long maxFileSize) {
            this.availablePlugins = availablePlugins;
            this.supportedOpenApiVersions = supportedOpenApiVersions;
            this.supportedSpecTypes = supportedSpecTypes;
            this.maxFileSize = maxFileSize;
        }
    }

    public static class ValidationResponse {
        public final boolean valid;
        public final String message;
        public final java.util.List<String> errors;
        public final java.util.List<String> warnings;
        public final String version = "2.0-plugin-enabled";

        public ValidationResponse(boolean valid, String message, java.util.List<String> errors,
                java.util.List<String> warnings) {
            this.valid = valid;
            this.message = message;
            this.errors = errors;
            this.warnings = warnings;
        }
    }
}
