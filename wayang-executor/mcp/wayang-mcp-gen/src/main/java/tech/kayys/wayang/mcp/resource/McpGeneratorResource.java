package tech.kayys.wayang.mcp.resource;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.mcp.config.McpServerConfig;
import tech.kayys.wayang.mcp.service.McpServerGeneratorService;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;

@Path("/api/mcp-generator")
@Produces(MediaType.APPLICATION_JSON)
public class McpGeneratorResource {

    @Inject
    McpServerGeneratorService generatorService;

    @Inject
    McpServerConfig config;

    public static class FileUpload {
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

        @RestForm("specType")
        @PartType(MediaType.TEXT_PLAIN)
        public String specType = "auto"; // auto, openapi, postman

        @RestForm("collectionName")
        @PartType(MediaType.TEXT_PLAIN)
        public String collectionName;

        @RestForm("includeAuth")
        @PartType(MediaType.TEXT_PLAIN)
        public String includeAuth;

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

        public SpecificationType getSpecType() {
            if (specType == null)
                return SpecificationType.AUTO;
            return switch (specType.toLowerCase()) {
                case "openapi" -> SpecificationType.OPENAPI;
                case "postman" -> SpecificationType.POSTMAN;
                default -> SpecificationType.AUTO;
            };
        }

        public String getCollectionNameOrDefault(String defaultValue) {
            return collectionName != null && !collectionName.trim().isEmpty() ? collectionName : defaultValue;
        }
    }

    public enum SpecificationType {
        AUTO, OPENAPI, POSTMAN
    }

    @POST
    @Path("/generate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> generateMcpServer(@BeanParam FileUpload upload) {
        Log.info("Starting MCP server generation for file: " + upload.filename);

        return generatorService.generateMcpServer(
                upload.file,
                upload.filename,
                upload.getPackageNameOrDefault(config.defaultPackage()),
                upload.getServerNameOrDefault(config.defaultServerName()),
                upload.getBaseUrlOrDefault(config.defaultBaseUrl()),
                upload.isIncludeAuth(),
                upload.getSpecType(),
                upload.getCollectionNameOrDefault("Generated Collection")).map(zipBytes -> {
                    String filename = upload.getServerNameOrDefault(config.defaultServerName()).toLowerCase()
                            + "-mcp-server.zip";
                    Log.info("Successfully generated MCP server ZIP: " + filename + " (" + zipBytes.length + " bytes)");

                    return Response.ok(zipBytes)
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                            .header("Content-Type", "application/zip")
                            .header("Content-Length", String.valueOf(zipBytes.length))
                            .build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Failed to generate MCP server", throwable);

                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Generation failed: " + throwable.getMessage()))
                            .type(MediaType.APPLICATION_JSON)
                            .build();
                });
    }

    @POST
    @Path("/validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> validateOpenApi(@BeanParam FileUpload upload) {
        Log.info("Validating OpenAPI specification: " + upload.filename);

        return generatorService.validateSpec(upload.file, upload.filename, upload.getSpecType())
                .map(result -> {
                    Log.info("Validation completed for: " + upload.filename);
                    return Response.ok(result).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Validation failed for: " + upload.filename, throwable);

                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Validation failed: " + throwable.getMessage()))
                            .build();
                });
    }

    @POST
    @Path("/preview")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> previewGeneration(@BeanParam FileUpload upload) {
        Log.info("Generating preview for: " + upload.filename);

        return generatorService.previewGeneration(
                upload.file,
                upload.filename,
                upload.getPackageNameOrDefault(config.defaultPackage()),
                upload.getServerNameOrDefault(config.defaultServerName()),
                upload.getSpecType(),
                upload.getCollectionNameOrDefault("Generated Collection")).map(preview -> {
                    Log.info("Preview generated for: " + upload.filename);
                    return Response.ok(preview).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Preview generation failed for: " + upload.filename, throwable);

                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Preview failed: " + throwable.getMessage()))
                            .build();
                });
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(new HealthResponse("UP", "MCP Generator Service is running")).build();
    }

    @GET
    @Path("/info")
    public Response info() {
        return Response.ok(new GeneratorInfo(
                "MCP Server Generator",
                "1.0.0",
                config.supportedVersions(),
                config.supportedSpecTypes(),
                config.maxFileSize(),
                config.defaultPackage(),
                config.defaultServerName())).build();
    }

    // Response DTOs
    public static class ErrorResponse {
        public final String error;
        public final long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class HealthResponse {
        public final String status;
        public final String message;
        public final long timestamp;

        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class GeneratorInfo {
        public final String name;
        public final String version;
        public final String[] supportedOpenApiVersions;
        public final String[] supportedSpecTypes;
        public final long maxFileSize;
        public final String defaultPackage;
        public final String defaultServerName;

        public GeneratorInfo(String name, String version, String[] supportedOpenApiVersions,
                String[] supportedSpecTypes, long maxFileSize, String defaultPackage, String defaultServerName) {
            this.name = name;
            this.version = version;
            this.supportedOpenApiVersions = supportedOpenApiVersions;
            this.supportedSpecTypes = supportedSpecTypes;
            this.maxFileSize = maxFileSize;
            this.defaultPackage = defaultPackage;
            this.defaultServerName = defaultServerName;
        }
    }
}
