package tech.kayys.wayang.resources;

import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.project.domain.IntegrationPattern;
import tech.kayys.wayang.project.dto.CreatePatternRequest;
import tech.kayys.wayang.project.dto.IntegrationExecutionResult;
import tech.kayys.wayang.project.service.ControlPlaneService;
import tech.kayys.wayang.security.service.IketSecurityService;

@Path("/api/v1/control-plane/integrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Control Plane - Integrations", description = "Enterprise integration patterns")
public class IntegrationResource {

        @Inject
        ControlPlaneService controlPlaneService;

        @Inject
        IketSecurityService iketSecurity;

        @POST
        @Operation(summary = "Create integration pattern")
        public Uni<RestResponse<IntegrationPattern>> createPattern(
                        @QueryParam("projectId") UUID projectId,
                        @Valid CreatePatternRequest request) {

                return controlPlaneService.createIntegrationPattern(projectId, request)
                                .map(pattern -> RestResponse.status(
                                                RestResponse.Status.CREATED, pattern));
        }

        @POST
        @Path("/{patternId}/execute")
        @Operation(summary = "Execute integration pattern")
        public Uni<RestResponse<IntegrationExecutionResult>> executePattern(
                        @PathParam("patternId") UUID patternId,
                        @Valid Map<String, Object> payload) {

                return controlPlaneService.executeIntegrationPattern(patternId, payload)
                                .map(RestResponse::ok);
        }
}
