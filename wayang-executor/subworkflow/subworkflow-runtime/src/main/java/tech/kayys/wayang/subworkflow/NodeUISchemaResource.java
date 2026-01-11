package tech.kayys.silat.ui;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.Map;

/**
 * REST API for UI Schema
 */
@Path("/api/v1/ui/schemas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "UI Schemas", description = "Node UI schema registry")
public class NodeUISchemaResource {

    @Inject
    NodeUISchemaRegistry registry;

    @GET
    @Operation(summary = "Get all node schemas")
    public Uni<Map<String, NodeUISchema>> getAllSchemas() {
        return Uni.createFrom().item(registry.getAllSchemas());
    }

    @GET
    @Path("/{nodeType}")
    @Operation(summary = "Get schema for specific node type")
    public Uni<RestResponse<NodeUISchema>> getSchema(
            @PathParam("nodeType") String nodeType) {

        return Uni.createFrom().item(
            registry.getSchema(nodeType)
                .map(RestResponse::ok)
                .orElse(RestResponse.notFound())
        );
    }

    @GET
    @Path("/category/{category}")
    @Operation(summary = "Get schemas by category")
    public Uni<Map<String, NodeUISchema>> getSchemasByCategory(
            @PathParam("category") String category) {

        return Uni.createFrom().item(
            registry.getSchemasByCategory(category)
        );
    }

    @GET
    @Path("/search")
    @Operation(summary = "Search schemas")
    public Uni<List<NodeUISchema>> searchSchemas(
            @QueryParam("q") String query) {

        return Uni.createFrom().item(
            registry.searchSchemas(query != null ? query : "")
        );
    }

    @GET
    @Path("/categories")
    @Operation(summary = "Get all categories")
    public Uni<List<CategoryInfo>> getCategories() {
        Map<String, Integer> categoryCounts = new HashMap<>();

        registry.getAllSchemas().values().forEach(schema -> {
            String category = schema.metadata().category();
            categoryCounts.merge(category, 1, Integer::sum);
        });

        List<CategoryInfo> categories = categoryCounts.entrySet().stream()
            .map(e -> new CategoryInfo(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(CategoryInfo::name))
            .toList();

        return Uni.createFrom().item(categories);
    }
}

record CategoryInfo(String name, int count) {}