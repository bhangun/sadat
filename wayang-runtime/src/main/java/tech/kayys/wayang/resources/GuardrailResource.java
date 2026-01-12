package tech.kayys.wayang.resources;

import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.dto.GuardrailTestRequest;
import tech.kayys.wayang.dto.GuardrailTestResponse;
import tech.kayys.wayang.guardrails.service.GuardrailEngine;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.IketSecurityService;

@Path("/api/v1/guardrails")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Guardrails", description = "Guardrail management")
@SecurityRequirement(name = "bearer-jwt")
public class GuardrailResource {

        @Inject
        GuardrailEngine guardrailEngine;

        @Inject
        IketSecurityService iketSecurity;

        @POST
        @Path("/test")
        @Operation(summary = "Test guardrails on content")
        @RolesAllowed({ "admin", "ai_engineer" })
        public Uni<GuardrailTestResponse> testGuardrails(
                        @Valid GuardrailTestRequest request) {

                AuthenticatedUser user = iketSecurity.getCurrentUser();

                return guardrailEngine.checkInput(
                                request.content(),
                                request.policy(),
                                user.userId(),
                                user.tenantId())
                                .map(inputResult -> guardrailEngine.checkOutput(
                                                request.content(),
                                                request.policy(),
                                                user.userId(),
                                                user.tenantId())
                                                .map(outputResult -> new GuardrailTestResponse(
                                                                inputResult,
                                                                outputResult,
                                                                Instant.now())))
                                .flatMap(uni -> uni);
        }
}
