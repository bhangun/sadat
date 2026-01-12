package tech.kayys.wayang.resources;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.project.domain.WorkflowTemplate;
import tech.kayys.wayang.project.dto.CatalogTemplate;
import tech.kayys.wayang.project.dto.PatternCatalogEntry;
import tech.kayys.wayang.project.service.TemplateCatalogService;
import tech.kayys.wayang.security.service.IketSecurityService;

@Path("/api/v1/control-plane/catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Control Plane - Catalog", description = "Template and pattern catalog")
public class CatalogResource {

    @Inject
    TemplateCatalogService catalogService;

    @Inject
    IketSecurityService iketSecurity;

    @GET
    @Path("/templates")
    @Operation(summary = "Browse template catalog")
    public Uni<List<CatalogTemplate>> browseTemplates(
            @QueryParam("category") String category,
            @QueryParam("search") String searchTerm) {

        return catalogService.browseTemplates(category, searchTerm);
    }

    @GET
    @Path("/templates/{templateId}")
    @Operation(summary = "Get template details")
    public Uni<RestResponse<CatalogTemplate>> getCatalogTemplate(
            @PathParam("templateId") String templateId) {

        return catalogService.getTemplate(templateId)
                .map(template -> template != null ? RestResponse.ok(template) : RestResponse.notFound());
    }

    @POST
    @Path("/templates/{templateId}/clone")
    @Operation(summary = "Clone catalog template to project")
    public Uni<RestResponse<WorkflowTemplate>> cloneTemplate(
            @PathParam("templateId") String templateId,
            @QueryParam("projectId") UUID projectId) {

        return catalogService.cloneToProject(templateId, projectId)
                .map(template -> RestResponse.status(
                        RestResponse.Status.CREATED, template));
    }

    @GET
    @Path("/patterns")
    @Operation(summary = "Browse EIP pattern catalog")
    public Uni<List<PatternCatalogEntry>> browsePatterns(
            @QueryParam("category") String category) {

        return catalogService.browsePatterns(category);
    }
}
