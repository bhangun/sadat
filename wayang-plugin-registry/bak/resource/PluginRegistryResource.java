
package tech.kayys.wayang.plugin.runtime.resource;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Plugin Registry Service - Central repository for plugin metadata
 * 
 * Responsibilities:
 * - CRUD operations for plugin descriptors
 * - Version management
 * - Capability indexing
 * - Query and discovery APIs
 * - Integration with governance and scanner services
 * 
 * Technology:
 * - Quarkus Reactive with Hibernate Reactive
 * - PostgreSQL with JSONB for flexible plugin metadata
 * - Reactive REST APIs using Mutiny
 * - Event emission to Kafka for audit trail
 */

@Path("/api/v1/plugins")
@Tag(name = "Plugin Registry", description = "Plugin registration and discovery")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class PluginRegistryResource {

    private static final Logger LOG = Logger.getLogger(PluginRegistryResource.class);

    @Inject
    PluginRegistryService registryService;

    @Inject
    PluginAuditService auditService;

    @Inject
    PluginValidationService validationService;

    /**
     * Register a new plugin
     * Validates descriptor, performs security checks, and emits audit event
     */
    @POST
    @Operation(summary = "Register new plugin", description = "Submit plugin descriptor for registration")
    public Uni<Response> registerPlugin(
            @Valid PluginDescriptor descriptor,
            @QueryParam("autoApprove") @DefaultValue("false") boolean autoApprove) {
        
        LOG.infof("Registering plugin: %s version %s", descriptor.getId(), descriptor.getVersion());

        return validationService.validateDescriptor(descriptor)
            .onItem().transformToUni(validationResult -> {
                if (!validationResult.isValid()) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("VALIDATION_ERROR", validationResult.getErrors()))
                            .build()
                    );
                }

                return registryService.registerPlugin(descriptor, autoApprove)
                    .onItem().transformToUni(plugin -> 
                        auditService.logEvent(
                            PluginAuditEvent.pluginRegistered(plugin)
                        ).replaceWith(plugin)
                    )
                    .onItem().transform(plugin -> 
                        Response.status(Response.Status.CREATED)
                            .entity(plugin)
                            .build()
                    )
                    .onFailure().recoverWithItem(throwable -> {
                        LOG.error("Failed to register plugin", throwable);
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(ErrorResponse.from(throwable))
                            .build();
                    });
            });
    }

    /**
     * Get plugin by ID and version
     */
    @GET
    @Path("/{pluginId}/{version}")
    @Operation(summary = "Get plugin descriptor")
    public Uni<Response> getPlugin(
            @PathParam("pluginId") String pluginId,
            @PathParam("version") String version) {
        
        return registryService.getPlugin(pluginId, version)
            .onItem().ifNotNull().transform(plugin -> 
                Response.ok(plugin).build()
            )
            .onItem().ifNull().continueWith(
                Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Plugin not found"))
                    .build()
            );
    }

    /**
     * List all plugins with filtering
     */
    @GET
    @Operation(summary = "List plugins", description = "Query plugins with filters")
    public Uni<Response> listPlugins(
            @QueryParam("status") String status,
            @QueryParam("capability") String capability,
            @QueryParam("tenantId") String tenantId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        PluginQuery query = PluginQuery.builder()
            .status(status)
            .capability(capability)
            .tenantId(tenantId)
            .page(page)
            .size(size)
            .build();

        return registryService.queryPlugins(query)
            .onItem().transform(result -> 
                Response.ok(result).build()
            );
    }

    /**
     * Update plugin status (approve, reject, revoke)
     */
    @PUT
    @Path("/{pluginId}/{version}/status")
    @Operation(summary = "Update plugin status")
    public Uni<Response> updateStatus(
            @PathParam("pluginId") String pluginId,
            @PathParam("version") String version,
            StatusUpdateRequest request) {
        
        return registryService.updateStatus(pluginId, version, request)
            .onItem().transformToUni(plugin -> 
                auditService.logEvent(
                    PluginAuditEvent.statusChanged(plugin, request)
                ).replaceWith(plugin)
            )
            .onItem().transform(plugin -> 
                Response.ok(plugin).build()
            )
            .onFailure().recoverWithItem(throwable -> 
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponse.from(throwable))
                    .build()
            );
    }

    /**
     * Delete plugin (soft delete - marks as deprecated)
     */
    @DELETE
    @Path("/{pluginId}/{version}")
    @Operation(summary = "Deprecate plugin")
    public Uni<Response> deletePlugin(
            @PathParam("pluginId") String pluginId,
            @PathParam("version") String version,
            @QueryParam("reason") String reason) {
        
        return registryService.deprecatePlugin(pluginId, version, reason)
            .onItem().transformToUni(success -> 
                auditService.logEvent(
                    PluginAuditEvent.pluginDeprecated(pluginId, version, reason)
                ).replaceWith(success)
            )
            .onItem().transform(success -> 
                Response.noContent().build()
            );
    }

    /**
     * Get plugin signature for verification
     */
    @GET
    @Path("/{pluginId}/{version}/signature")
    @Operation(summary = "Get plugin signature")
    public Uni<Response> getSignature(
            @PathParam("pluginId") String pluginId,
            @PathParam("version") String version) {
        
        return registryService.getSignature(pluginId, version)
            .onItem().ifNotNull().transform(signature -> 
                Response.ok(signature).build()
            )
            .onItem().ifNull().continueWith(
                Response.status(Response.Status.NOT_FOUND).build()
            );
    }
}