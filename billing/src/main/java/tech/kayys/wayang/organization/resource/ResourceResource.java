package tech.kayys.wayang.organization.resource;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.billing.domain.ResourceAllocation;
import tech.kayys.wayang.billing.dto.ResourceHealth;
import tech.kayys.wayang.billing.dto.ScaleResourceRequest;
import tech.kayys.wayang.billing.model.ResourceType;
import tech.kayys.wayang.billing.service.ProvisioningService;

@Path("/api/v1/management/resources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearer")
@Tag(name = "Resources", description = "Resource provisioning and management")
public class ResourceResource {

    @Inject
    ProvisioningService provisioningService;

    /**
     * List resources for organization
     */
    @GET
    @Path("/organizations/{organizationId}")
    @Operation(summary = "List organization resources")
    public Uni<List<ResourceAllocation>> listResources(
            @PathParam("organizationId") UUID organizationId,
            @QueryParam("type") ResourceType type) {

        return ResourceAllocation.<ResourceAllocation>find(
                type != null ? "organization.organizationId = ?1 and resourceType = ?2"
                        : "organization.organizationId = ?1",
                type != null ? List.of(organizationId, type).toArray() : organizationId).list();
    }

    /**
     * Get resource details
     */
    @GET
    @Path("/{allocationId}")
    @Operation(summary = "Get resource details")
    public Uni<RestResponse<ResourceAllocation>> getResource(
            @PathParam("allocationId") UUID allocationId) {

        return ResourceAllocation.<ResourceAllocation>findById(allocationId)
                .map(res -> res != null ? RestResponse.ok(res) : RestResponse.notFound());
    }

    /**
     * Scale resource
     */
    @POST
    @Path("/{allocationId}/scale")
    @Operation(summary = "Scale resource")
    public Uni<RestResponse<Void>> scaleResource(
            @PathParam("allocationId") UUID allocationId,
            @Valid ScaleResourceRequest request) {

        return ResourceAllocation.<ResourceAllocation>findById(allocationId)
                .flatMap(resource -> provisioningService.scaleResources(
                        resource.organization,
                        resource.resourceType,
                        request.newCapacity()))
                .map(v -> RestResponse.accepted());
    }

    /**
     * Get resource health
     */
    @GET
    @Path("/{allocationId}/health")
    @Operation(summary = "Get resource health")
    public Uni<RestResponse<ResourceHealth>> getResourceHealth(
            @PathParam("allocationId") UUID allocationId) {

        return ResourceAllocation.<ResourceAllocation>findById(allocationId)
                .map(resource -> {
                    if (resource == null) {
                        return RestResponse.<ResourceHealth>notFound();
                    }

                    ResourceHealth health = new ResourceHealth(
                            resource.resourceId,
                            resource.healthStatus,
                            resource.lastHealthCheck,
                            resource.getUtilizationPercent());

                    return RestResponse.ok(health);
                });
    }
}
