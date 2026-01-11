package tech.kayys.wayang.mcp.template;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TemplateResource {

    @Inject
    CustomTemplateManager templateManager;

    @GET
    @Path("/sets")
    public Response getTemplateSets() {
        Log.info("Listing template sets");

        try {
            List<TemplateSet> templateSets = templateManager.getAvailableTemplateSets();

            List<TemplateSetSummary> summaries = templateSets.stream()
                    .map(set -> new TemplateSetSummary(
                            set.getId(),
                            set.getName(),
                            set.getDescription(),
                            set.getVersion(),
                            set.getTemplateCount()))
                    .toList();

            return Response.ok(new TemplateSetListResponse(summaries)).build();

        } catch (Exception e) {
            Log.error("Failed to list template sets", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to list template sets: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/sets/{setId}")
    public Response getTemplateSet(@PathParam("setId") String setId) {
        Log.infof("Getting template set: %s", setId);

        try {
            var templateSet = templateManager.getTemplateSet(setId);

            if (templateSet.isPresent()) {
                TemplateSet set = templateSet.get();

                List<TemplateSummary> templates = set.getTemplates().stream()
                        .map(template -> new TemplateSummary(
                                template.getId(),
                                template.getName(),
                                template.getFileType(),
                                template.getProperties()))
                        .toList();

                return Response.ok(new TemplateSetDetailsResponse(
                        set.getId(),
                        set.getName(),
                        set.getDescription(),
                        set.getVersion(),
                        templates)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Template set not found: " + setId))
                        .build();
            }

        } catch (Exception e) {
            Log.error("Failed to get template set", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get template set: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{templateId}")
    public Response getTemplate(@PathParam("templateId") String templateId) {
        Log.infof("Getting template: %s", templateId);

        try {
            var template = templateManager.getTemplate(templateId);

            if (template.isPresent()) {
                CustomTemplate t = template.get();

                return Response.ok(new TemplateDetailsResponse(
                        t.getId(),
                        t.getName(),
                        t.getFileType(),
                        t.getContent(),
                        t.getProperties())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Template not found: " + templateId))
                        .build();
            }

        } catch (Exception e) {
            Log.error("Failed to get template", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get template: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/")
    public Uni<Response> createTemplate(CreateTemplateRequest request) {
        Log.infof("Creating custom template: %s", request.name);

        return Uni.createFrom().item(() -> {
            try {
                CustomTemplate template = new CustomTemplate(
                        request.id,
                        request.name,
                        request.fileType,
                        request.content,
                        request.properties != null ? request.properties : Map.of());

                templateManager.saveUserTemplate(template);

                return Response.status(Response.Status.CREATED)
                        .entity(new SuccessResponse(
                                "Template created successfully",
                                Map.of("templateId", template.getId())))
                        .build();

            } catch (Exception e) {
                Log.error("Failed to create template", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Failed to create template: " + e.getMessage()))
                        .build();
            }
        });
    }

    @PUT
    @Path("/{templateId}")
    public Uni<Response> updateTemplate(@PathParam("templateId") String templateId,
            UpdateTemplateRequest request) {
        Log.infof("Updating template: %s", templateId);

        return Uni.createFrom().item(() -> {
            try {
                var existingTemplate = templateManager.getTemplate(templateId);

                if (existingTemplate.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Template not found: " + templateId))
                            .build();
                }

                CustomTemplate updatedTemplate = new CustomTemplate(
                        templateId,
                        request.name != null ? request.name : existingTemplate.get().getName(),
                        request.fileType != null ? request.fileType : existingTemplate.get().getFileType(),
                        request.content != null ? request.content : existingTemplate.get().getContent(),
                        request.properties != null ? request.properties : existingTemplate.get().getProperties());

                templateManager.saveUserTemplate(updatedTemplate);

                return Response.ok(new SuccessResponse(
                        "Template updated successfully",
                        Map.of("templateId", templateId))).build();

            } catch (Exception e) {
                Log.error("Failed to update template", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Failed to update template: " + e.getMessage()))
                        .build();
            }
        });
    }

    @DELETE
    @Path("/{templateId}")
    public Uni<Response> deleteTemplate(@PathParam("templateId") String templateId) {
        Log.infof("Deleting template: %s", templateId);

        return Uni.createFrom().item(() -> {
            try {
                templateManager.deleteUserTemplate(templateId);

                return Response.ok(new SuccessResponse(
                        "Template deleted successfully",
                        Map.of("templateId", templateId))).build();

            } catch (Exception e) {
                Log.error("Failed to delete template", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Failed to delete template: " + e.getMessage()))
                        .build();
            }
        });
    }

    @POST
    @Path("/{templateId}/render")
    public Response renderTemplate(@PathParam("templateId") String templateId,
            Map<String, Object> templateData) {
        Log.infof("Rendering template: %s", templateId);

        try {
            TemplateRenderResult result = templateManager.renderTemplate(templateId, templateData);

            if (result.isSuccess()) {
                return Response.ok(new TemplateRenderResponse(
                        true,
                        result.getContent(),
                        null)).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new TemplateRenderResponse(
                                false,
                                null,
                                result.getError()))
                        .build();
            }

        } catch (Exception e) {
            Log.error("Failed to render template", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Template rendering failed: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/framework/{framework}")
    public Response getTemplatesForFramework(@PathParam("framework") String framework) {
        Log.infof("Getting templates for framework: %s", framework);

        try {
            List<CustomTemplate> templates = templateManager.getTemplatesForFramework(framework);

            List<TemplateSummary> summaries = templates.stream()
                    .map(template -> new TemplateSummary(
                            template.getId(),
                            template.getName(),
                            template.getFileType(),
                            template.getProperties()))
                    .toList();

            return Response.ok(new FrameworkTemplatesResponse(
                    framework,
                    summaries.size(),
                    summaries)).build();

        } catch (Exception e) {
            Log.error("Failed to get templates for framework", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get framework templates: " + e.getMessage()))
                    .build();
        }
    }

    // Request/Response DTOs
    public static class CreateTemplateRequest {
        public String id;
        public String name;
        public String fileType;
        public String content;
        public Map<String, Object> properties;
    }

    public static class UpdateTemplateRequest {
        public String name;
        public String fileType;
        public String content;
        public Map<String, Object> properties;
    }

    public static class TemplateSetListResponse {
        public final List<TemplateSetSummary> templateSets;
        public final int count;

        public TemplateSetListResponse(List<TemplateSetSummary> templateSets) {
            this.templateSets = templateSets;
            this.count = templateSets.size();
        }
    }

    public static class TemplateSetSummary {
        public final String id;
        public final String name;
        public final String description;
        public final String version;
        public final int templateCount;

        public TemplateSetSummary(String id, String name, String description, String version, int templateCount) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.version = version;
            this.templateCount = templateCount;
        }
    }

    public static class TemplateSetDetailsResponse {
        public final String id;
        public final String name;
        public final String description;
        public final String version;
        public final List<TemplateSummary> templates;

        public TemplateSetDetailsResponse(String id, String name, String description, String version,
                List<TemplateSummary> templates) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.version = version;
            this.templates = templates;
        }
    }

    public static class TemplateSummary {
        public final String id;
        public final String name;
        public final String fileType;
        public final Map<String, Object> properties;

        public TemplateSummary(String id, String name, String fileType, Map<String, Object> properties) {
            this.id = id;
            this.name = name;
            this.fileType = fileType;
            this.properties = properties;
        }
    }

    public static class TemplateDetailsResponse {
        public final String id;
        public final String name;
        public final String fileType;
        public final String content;
        public final Map<String, Object> properties;

        public TemplateDetailsResponse(String id, String name, String fileType, String content,
                Map<String, Object> properties) {
            this.id = id;
            this.name = name;
            this.fileType = fileType;
            this.content = content;
            this.properties = properties;
        }
    }

    public static class TemplateRenderResponse {
        public final boolean success;
        public final String content;
        public final String error;

        public TemplateRenderResponse(boolean success, String content, String error) {
            this.success = success;
            this.content = content;
            this.error = error;
        }
    }

    public static class FrameworkTemplatesResponse {
        public final String framework;
        public final int count;
        public final List<TemplateSummary> templates;

        public FrameworkTemplatesResponse(String framework, int count, List<TemplateSummary> templates) {
            this.framework = framework;
            this.count = count;
            this.templates = templates;
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
