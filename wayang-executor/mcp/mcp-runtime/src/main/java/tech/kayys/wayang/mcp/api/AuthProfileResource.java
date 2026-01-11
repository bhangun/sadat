package tech.kayys.wayang.mcp.api;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.mcp.domain.*;
import tech.kayys.wayang.mcp.dto.AuthLocation;
import tech.kayys.wayang.mcp.dto.AuthProfileResponse;
import tech.kayys.wayang.mcp.dto.AuthType;
import tech.kayys.wayang.mcp.dto.CreateAuthProfileRequest;
import tech.kayys.wayang.mcp.dto.TenantContext;
import tech.kayys.wayang.mcp.repository.AuthProfileRepository;

import java.util.*;

/**
 * Auth Profile Resource API
 */
@Path("/api/v1/mcp/auth-profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "MCP Auth Profiles", description = "Authentication profile management")
public class AuthProfileResource {

    @Inject
    TenantContext tenantContext;

    @Inject
    tech.kayys.wayang.mcp.security.VaultSecretManager vaultManager;

    @Inject
    AuthProfileRepository authProfileRepository;

    /**
     * Create auth profile
     */
    @POST
    @Operation(summary = "Create authentication profile")
    public Uni<RestResponse<AuthProfileResponse>> createAuthProfile(
            @Valid CreateAuthProfileRequest request) {

        String tenantId = tenantContext.getCurrentTenantId();

        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            AuthProfile profile = new AuthProfile();
            profile.setProfileId(UUID.randomUUID().toString());
            profile.setTenantId(tenantId);
            profile.setProfileName(request.profileName());
            profile.setAuthType(AuthType.valueOf(request.authType()));
            profile.setDescription(request.description());

            // Configure auth
            AuthConfig config = new AuthConfig();
            config.setLocation(AuthLocation.valueOf(request.location()));
            config.setParamName(request.paramName());
            config.setScheme(request.scheme());
            profile.setConfig(config);

            // Store secret in Vault
            String vaultPath = "wayang/mcp/" + tenantId + "/" + profile.getProfileId();

            return vaultManager.storeSecret(vaultPath, request.secretValue())
                    .flatMap(v -> {
                        profile.setVaultPath(vaultPath);
                        profile.setSecretKey("auth_secret");
                        profile.setEnabled(true);
                        profile.setCreatedAt(java.time.Instant.now());
                        profile.setUpdatedAt(java.time.Instant.now());

                        return authProfileRepository.save(profile);
                    })
                    .map(p -> RestResponse.status(
                            RestResponse.Status.CREATED,
                            new AuthProfileResponse(
                                    profile.getProfileId(),
                                    profile.getProfileName(),
                                    profile.getAuthType().name(),
                                    profile.isEnabled())));
        });
    }

    /**
     * List auth profiles
     */
    @GET
    @Operation(summary = "List authentication profiles")
    public Uni<List<AuthProfileResponse>> listAuthProfiles() {
        String tenantId = tenantContext.getCurrentTenantId();

        return authProfileRepository.findByTenantIdAndEnabled(tenantId, true)
                .map(profiles -> profiles.stream()
                        .map(p -> new AuthProfileResponse(
                                p.getProfileId(),
                                p.getProfileName(),
                                p.getAuthType().name(),
                                p.isEnabled()))
                        .toList());
    }
}