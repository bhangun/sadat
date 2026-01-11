package tech.kayys.wayang.integration.designer;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * REST API for visual route designer
 */
@Path("/api/v1/designer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class VisualRouteDesignerAPI {

    private static final Logger LOG = LoggerFactory.getLogger(VisualRouteDesignerAPI.class);

    @Inject
    VisualRouteDesignerService designerService;

    @Inject
    RouteValidationService validationService;

    @Inject
    RouteOptimizationService optimizationService;

    @Inject
    TemplateMarketplaceService marketplaceService;

    /**
     * Create new route design
     */
    @POST
    @Path("/routes")
    public Uni<RouteDesign> createRoute(CreateRouteRequest request) {
        LOG.info("Creating new route design: {}", request.name());

        return designerService.createRoute(
            request.name(),
            request.description(),
            request.category(),
            request.tenantId()
        );
    }

    /**
     * Get route design
     */
    @GET
    @Path("/routes/{routeId}")
    public Uni<RouteDesign> getRoute(@PathParam("routeId") String routeId) {
        return designerService.getRoute(routeId);
    }

    /**
     * Update route design
     */
    @PUT
    @Path("/routes/{routeId}")
    public Uni<RouteDesign> updateRoute(
            @PathParam("routeId") String routeId,
            UpdateRouteRequest request) {

        return designerService.updateRoute(routeId, request);
    }

    /**
     * Add node to route
     */
    @POST
    @Path("/routes/{routeId}/nodes")
    public Uni<DesignNode> addNode(
            @PathParam("routeId") String routeId,
            AddNodeRequest request) {

        return designerService.addNode(
            routeId,
            request.nodeType(),
            request.label(),
            request.position(),
            request.configuration()
        );
    }

    /**
     * Add connection between nodes
     */
    @POST
    @Path("/routes/{routeId}/connections")
    public Uni<DesignConnection> addConnection(
            @PathParam("routeId") String routeId,
            AddConnectionRequest request) {

        return designerService.addConnection(
            routeId,
            request.sourceNodeId(),
            request.targetNodeId(),
            request.connectionType(),
            request.condition()
        );
    }

    /**
     * Validate route design
     */
    @POST
    @Path("/routes/{routeId}/validate")
    public Uni<ValidationResult> validateRoute(@PathParam("routeId") String routeId) {
        return validationService.validateRoute(routeId);
    }

    /**
     * Generate Camel DSL from design
     */
    @POST
    @Path("/routes/{routeId}/generate")
    public Uni<GeneratedRoute> generateRoute(@PathParam("routeId") String routeId) {
        return designerService.generateCamelRoute(routeId);
    }

    /**
     * Deploy route
     */
    @POST
    @Path("/routes/{routeId}/deploy")
    public Uni<DeploymentResult> deployRoute(@PathParam("routeId") String routeId) {
        return designerService.deployRoute(routeId);
    }

    /**
     * Get AI-powered optimization suggestions
     */
    @POST
    @Path("/routes/{routeId}/optimize")
    public Uni<OptimizationSuggestions> getOptimizationSuggestions(
            @PathParam("routeId") String routeId) {

        return optimizationService.analyzeAndOptimize(routeId);
    }

    /**
     * Search template marketplace
     */
    @GET
    @Path("/marketplace/templates")
    public Uni<List<RouteTemplate>> searchTemplates(
            @QueryParam("category") String category,
            @QueryParam("search") String searchTerm) {

        return marketplaceService.searchTemplates(category, searchTerm);
    }

    /**
     * Clone template
     */
    @POST
    @Path("/marketplace/templates/{templateId}/clone")
    public Uni<RouteDesign> cloneTemplate(
            @PathParam("templateId") String templateId,
            @QueryParam("tenantId") String tenantId) {

        return marketplaceService.cloneTemplate(templateId, tenantId);
    }
}