package tech.kayys.wayang.resources;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import tech.kayys.wayang.audit.AuditLogEntry;
import tech.kayys.wayang.audit.AuditLogger;
import tech.kayys.wayang.security.dto.ApiKeyInfo;
import tech.kayys.wayang.security.dto.ApiKeyResponse;
import tech.kayys.wayang.security.dto.CreateApiKeyRequest;
import tech.kayys.wayang.security.service.ApiKeyService;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.KeycloakSecurityService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * ============================================================================
 * COMPLETE SILAT CONTROL PLANE API
 * ============================================================================
 * 
 * Secured REST API with:
 * - Keycloak OIDC authentication
 * - API Key support
 * - Role-based access control
 * - Guardrails integration
 * - WebSocket notifications
 * - Audit logging
 */

// ==================== API KEY MANAGEMENT API ====================

@Path("/api/v1/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "API Keys", description = "API key management")
@SecurityRequirement(name = "bearer-jwt")
public class ApiKeyResource {

    @Inject
    ApiKeyService apiKeyService;

    @Inject
    KeycloakSecurityService keycloakSecurity;

    @Inject
    AuditLogger auditLogger;

    @POST
    @Operation(summary = "Generate new API key")
    @RolesAllowed({ "admin", "developer" })
    public Uni<RestResponse<ApiKeyResponse>> generateKey(
            @Valid CreateApiKeyRequest request,
            @Context UriInfo uriInfo) {

        AuthenticatedUser user = keycloakSecurity.getCurrentUser();

        return apiKeyService.generateApiKey(request)
                .flatMap(response -> {
                    // Audit log
                    return auditLogger.log(new AuditLogEntry(
                            user.tenantId(),
                            user.userId(),
                            "CREATE_API_KEY",
                            "api_key",
                            response.id().toString(),
                            "SUCCESS",
                            extractIp(uriInfo),
                            extractUserAgent(uriInfo),
                            Map.of("name", request.name(), "scopes", request.scopes())))
                            .map(v -> RestResponse.status(
                                    RestResponse.Status.CREATED, response));
                });
    }

    @GET
    @Operation(summary = "List API keys")
    public Uni<List<ApiKeyInfo>> listKeys() {
        return apiKeyService.listApiKeys();
    }

    @DELETE
    @Path("/{keyId}")
    @Operation(summary = "Revoke API key")
    @RolesAllowed({ "admin", "developer" })
    public Uni<RestResponse<Void>> revokeKey(
            @PathParam("keyId") UUID keyId,
            @Context UriInfo uriInfo) {

        AuthenticatedUser user = keycloakSecurity.getCurrentUser();

        return apiKeyService.revokeApiKey(keyId)
                .flatMap(v -> auditLogger.log(new AuditLogEntry(
                        user.tenantId(),
                        user.userId(),
                        "REVOKE_API_KEY",
                        "api_key",
                        keyId.toString(),
                        "SUCCESS",
                        extractIp(uriInfo),
                        extractUserAgent(uriInfo),
                        Map.of())))
                .map(v -> RestResponse.noContent());
    }

    private String extractIp(UriInfo uriInfo) {
        return "127.0.0.1"; // In production, extract from X-Forwarded-For
    }

    private String extractUserAgent(UriInfo uriInfo) {
        return "Unknown"; // In production, extract from headers
    }
}
