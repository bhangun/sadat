package tech.kayys.wayang.organization.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import tech.kayys.wayang.billing.dto.UpdateOrganizationRequest;
import tech.kayys.wayang.billing.service.ProvisioningService;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.dto.CreateOrganizationRequest;
import tech.kayys.wayang.organization.dto.OrganizationStats;
import tech.kayys.wayang.organization.model.OrganizationStatus;
import tech.kayys.wayang.organization.service.OrganizationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

/**
 * ============================================================================
 * SILAT MANAGEMENT PLATFORM - REST API
 * ============================================================================
 * 
 * Comprehensive management API for:
 * - Organization management
 * - Subscription & billing
 * - Usage tracking & analytics
 * - Resource provisioning
 * - Administrative operations
 */

// ==================== ORGANIZATION MANAGEMENT API ====================

@Path("/api/v1/management/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearer")
@Tag(name = "Organizations", description = "Organization management operations")
public class OrganizationResource {

    @Inject
    OrganizationService organizationService;

    @Inject
    ProvisioningService provisioningService;

    /**
     * Create new organization
     */
    @POST
    @Operation(summary = "Create organization")
    public Uni<RestResponse<Organization>> createOrganization(
            @Valid CreateOrganizationRequest request) {

        return organizationService.createOrganization(request)
                .flatMap(org ->
                // Trigger provisioning
                provisioningService.onboardTenant(org)
                        .replaceWith(org))
                .map(org -> RestResponse.status(RestResponse.Status.CREATED, org))
                .onFailure().recoverWithItem(error -> RestResponse.serverError());
    }

    /**
     * Get organization by ID
     */
    @GET
    @Path("/{organizationId}")
    @Operation(summary = "Get organization")
    public Uni<RestResponse<Organization>> getOrganization(
            @PathParam("organizationId") UUID organizationId) {

        return organizationService.getOrganization(organizationId)
                .map(org -> org != null ? RestResponse.ok(org) : RestResponse.notFound());
    }

    /**
     * Update organization
     */
    @PUT
    @Path("/{organizationId}")
    @Operation(summary = "Update organization")
    public Uni<RestResponse<Organization>> updateOrganization(
            @PathParam("organizationId") UUID organizationId,
            @Valid UpdateOrganizationRequest request) {

        return organizationService.updateOrganization(organizationId, request)
                .map(RestResponse::ok);
    }

    /**
     * Suspend organization
     */
    @POST
    @Path("/{organizationId}/suspend")
    @Operation(summary = "Suspend organization")
    public Uni<RestResponse<Organization>> suspendOrganization(
            @PathParam("organizationId") UUID organizationId,
            @QueryParam("reason") String reason) {

        return organizationService.suspendOrganization(organizationId, reason)
                .map(RestResponse::ok);
    }

    /**
     * Activate organization
     */
    @POST
    @Path("/{organizationId}/activate")
    @Operation(summary = "Activate organization")
    public Uni<RestResponse<Organization>> activateOrganization(
            @PathParam("organizationId") UUID organizationId) {

        return organizationService.activateOrganization(organizationId)
                .map(RestResponse::ok);
    }

    /**
     * Delete organization
     */
    @DELETE
    @Path("/{organizationId}")
    @Operation(summary = "Delete organization")
    public Uni<RestResponse<Void>> deleteOrganization(
            @PathParam("organizationId") UUID organizationId) {

        return organizationService.deleteOrganization(organizationId)
                .flatMap(org ->
                // Deprovision all resources
                provisioningService.deprovisionTenant(org))
                .map(v -> RestResponse.noContent());
    }

    /**
     * List organizations
     */
    @GET
    @Operation(summary = "List organizations")
    public Uni<List<Organization>> listOrganizations(
            @QueryParam("status") OrganizationStatus status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        return organizationService.listOrganizations(status, page, size);
    }

    /**
     * Get organization statistics
     */
    @GET
    @Path("/{organizationId}/stats")
    @Operation(summary = "Get organization statistics")
    public Uni<OrganizationStats> getOrganizationStats(
            @PathParam("organizationId") UUID organizationId) {

        return organizationService.getOrganizationStats(organizationId);
    }
}