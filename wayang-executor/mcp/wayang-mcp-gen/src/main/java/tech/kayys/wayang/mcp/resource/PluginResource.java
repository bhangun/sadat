package tech.kayys.wayang.mcp.resource;

import tech.kayys.wayang.mcp.plugin.*;
import tech.kayys.wayang.mcp.service.PluginAwareGeneratorService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/plugins")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PluginResource {

    @Inject
    PluginAwareGeneratorService pluginService;

    @Inject
    PluginManager pluginManager;

    @GET
    @Path("/")
    public Response listPlugins() {
        Log.info("Listing available plugins");

        try {
            List<PluginAwareGeneratorService.PluginInfo> plugins = pluginService.getAvailablePlugins();

            return Response.ok(new PluginListResponse(
                    plugins.size(),
                    plugins)).build();

        } catch (Exception e) {
            Log.error("Failed to list plugins", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to list plugins: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{pluginId}")
    public Response getPlugin(@PathParam("pluginId") String pluginId) {
        Log.infof("Getting plugin details: %s", pluginId);

        try {
            var plugin = pluginManager.getPlugin(pluginId);

            if (plugin.isPresent()) {
                GeneratorPlugin p = plugin.get();
                return Response.ok(new PluginDetailsResponse(
                        p.getId(),
                        p.getName(),
                        p.getVersion(),
                        p.getDescription(),
                        p.getConfiguration())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Plugin not found: " + pluginId))
                        .build();
            }

        } catch (Exception e) {
            Log.error("Failed to get plugin details", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get plugin: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{pluginId}/configure")
    public Response configurePlugin(@PathParam("pluginId") String pluginId,
            Map<String, Object> configuration) {
        Log.infof("Configuring plugin: %s", pluginId);

        try {
            var plugin = pluginManager.getPlugin(pluginId);

            if (plugin.isPresent()) {
                plugin.get().configure(configuration);

                return Response.ok(new SuccessResponse(
                        "Plugin configured successfully",
                        Map.of("pluginId", pluginId, "configuration", configuration))).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Plugin not found: " + pluginId))
                        .build();
            }

        } catch (Exception e) {
            Log.error("Failed to configure plugin", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Failed to configure plugin: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/template-processors")
    public Response getTemplateProcessors() {
        Log.info("Listing template processors");

        try {
            // Get all template processors
            List<TemplateProcessor> processors = pluginManager.getTemplateProcessors("*");

            List<TemplateProcessorInfo> processorInfos = processors.stream()
                    .map(processor -> new TemplateProcessorInfo(
                            processor.getTemplateType(),
                            processor.getClass().getSimpleName()))
                    .toList();

            return Response.ok(new TemplateProcessorListResponse(
                    processorInfos.size(),
                    processorInfos)).build();

        } catch (Exception e) {
            Log.error("Failed to list template processors", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to list template processors: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/validators")
    public Response getValidators() {
        Log.info("Listing validators");

        try {
            List<ValidationPlugin> validators = pluginManager.getValidators("*");

            List<ValidatorInfo> validatorInfos = validators.stream()
                    .map(validator -> new ValidatorInfo(
                            validator.getValidationType(),
                            validator.getClass().getSimpleName(),
                            validator.getValidationRules().size()))
                    .toList();

            return Response.ok(new ValidatorListResponse(
                    validatorInfos.size(),
                    validatorInfos)).build();

        } catch (Exception e) {
            Log.error("Failed to list validators", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to list validators: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/execute/{pluginId}")
    public Uni<Response> executePlugin(@PathParam("pluginId") String pluginId,
            PluginExecutionRequest request) {
        Log.infof("Executing plugin: %s", pluginId);

        return Uni.createFrom().item(() -> {
            try {
                var plugin = pluginManager.getPlugin(pluginId);

                if (plugin.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Plugin not found: " + pluginId))
                            .build();
                }

                PluginExecutionContext context = pluginManager.createExecutionContext();
                context.setConfiguration(request.configuration != null ? request.configuration : new HashMap<>());

                // Set request data as context attributes
                if (request.data != null) {
                    request.data.forEach(context::setAttribute);
                }

                PluginResult result = plugin.get().execute(context);

                return Response.ok(new PluginExecutionResponse(
                        result.isSuccess(),
                        result.getMessage(),
                        result.getData(),
                        context.getExecutionTime())).build();

            } catch (Exception e) {
                Log.error("Failed to execute plugin", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse("Plugin execution failed: " + e.getMessage()))
                        .build();
            }
        });
    }

    // Response DTOs
    public static class PluginListResponse {
        public final int count;
        public final List<PluginAwareGeneratorService.PluginInfo> plugins;

        public PluginListResponse(int count, List<PluginAwareGeneratorService.PluginInfo> plugins) {
            this.count = count;
            this.plugins = plugins;
        }
    }

    public static class PluginDetailsResponse {
        public final String id;
        public final String name;
        public final String version;
        public final String description;
        public final Map<String, Object> configuration;

        public PluginDetailsResponse(String id, String name, String version, String description,
                Map<String, Object> configuration) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
            this.configuration = configuration;
        }
    }

    public static class TemplateProcessorListResponse {
        public final int count;
        public final List<TemplateProcessorInfo> processors;

        public TemplateProcessorListResponse(int count, List<TemplateProcessorInfo> processors) {
            this.count = count;
            this.processors = processors;
        }
    }

    public static class TemplateProcessorInfo {
        public final String templateType;
        public final String className;

        public TemplateProcessorInfo(String templateType, String className) {
            this.templateType = templateType;
            this.className = className;
        }
    }

    public static class ValidatorListResponse {
        public final int count;
        public final List<ValidatorInfo> validators;

        public ValidatorListResponse(int count, List<ValidatorInfo> validators) {
            this.count = count;
            this.validators = validators;
        }
    }

    public static class ValidatorInfo {
        public final String validationType;
        public final String className;
        public final int rulesCount;

        public ValidatorInfo(String validationType, String className, int rulesCount) {
            this.validationType = validationType;
            this.className = className;
            this.rulesCount = rulesCount;
        }
    }

    public static class PluginExecutionRequest {
        public Map<String, Object> configuration;
        public Map<String, Object> data;
        public String operation;
    }

    public static class PluginExecutionResponse {
        public final boolean success;
        public final String message;
        public final Map<String, Object> data;
        public final long executionTime;

        public PluginExecutionResponse(boolean success, String message, Map<String, Object> data,
                long executionTime) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.executionTime = executionTime;
        }
    }

    public static class ErrorResponse {
        public final String error;
        public final long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class SuccessResponse {
        public final String message;
        public final Map<String, Object> data;
        public final long timestamp;

        public SuccessResponse(String message, Map<String, Object> data) {
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
