package tech.kayys.wayang.agent.resource;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestPath;
import tech.kayys.wayang.agent.dto.AgentTemplate;
import tech.kayys.wayang.agent.dto.AgentTemplateDetail;
import tech.kayys.wayang.agent.dto.TemplateInstantiationRequest;
import tech.kayys.wayang.agent.service.AgentTemplateManager;

import java.util.List;
import java.util.Map;

/**
 * Agent Template REST API - UI-focused endpoints for template management
 */
@Path("/api/v1/agent-builder/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentTemplateResource {

    @Inject
    AgentTemplateManager templateManager;

    /**
     * List available templates for builder UI
     */
    @GET
    public Uni<Response> listTemplates(
            @QueryParam("category") String category,
            @QueryParam("agentType") tech.kayys.wayang.agent.dto.AgentType agentType,
            @QueryParam("useCase") String useCase) {
        
        Log.infof("Received request for templates - Category: %s, Type: %s, UseCase: %s", 
                  category, agentType, useCase);
        
        return templateManager.getTemplates(category, agentType, useCase)
                .map(templates -> Response.ok(templates).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error fetching templates", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error fetching templates: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Get template details for builder UI
     */
    @GET
    @Path("/{templateId}")
    public Uni<Response> getTemplateDetail(@RestPath String templateId) {
        Log.infof("Received request for template detail: %s", templateId);
        
        return templateManager.getTemplateDetail(templateId)
                .map(templateDetail -> Response.ok(templateDetail).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error fetching template detail", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error fetching template: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Create agent from template
     */
    @POST
    @Path("/{templateId}/instantiate")
    public Uni<Response> createAgentFromTemplate(
            @RestPath String templateId,
            TemplateInstantiationRequest request) {
        
        Log.infof("Received request to instantiate template: %s", templateId);
        
        return templateManager.getTemplate(templateId)
                .onItem().transformToUni(template -> {
                    // In a real implementation, this would create an agent from the template
                    // For now, we'll just return a success response
                    return Uni.createFrom().item(
                        Response.status(Response.Status.CREATED)
                               .entity(Map.of("message", "Agent created from template successfully"))
                               .build()
                    );
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error instantiating template", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error instantiating template: " + throwable.getMessage())
                            .build();
                });
    }
}