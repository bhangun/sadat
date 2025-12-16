package tech.kayys.wayang.client;

import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * GuardrailsClient - Client for Guardrails service
 */
@RegisterRestClient(configKey = "guardrails-service")
@Path("/api/v1/guardrails")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GuardrailsClient {

        /**
         * Validate workflow against guardrails
         */
        @POST
        @Path("/validate")
        @Retry(maxRetries = 2)
        @Timeout(value = 15000)
        Uni<GuardrailResult> validateWorkflow(GuardrailRequest request);

        /**
         * Check specific policy
         */
        @POST
        @Path("/check")
        @Timeout(value = 5000)
        Uni<PolicyCheckResult> checkPolicy(PolicyCheckRequest request);

        // DTOs
        record GuardrailRequest(
                        String workflowId,
                        String tenantId,
                        Object content,
                        List<String> policies) {
        }

        record GuardrailResult(
                        boolean allowed,
                        List<PolicyViolation> violations,
                        List<String> warnings) {
        }

        record PolicyViolation(
                        String policyId,
                        String severity,
                        String message,
                        String path) {
        }

        record PolicyCheckRequest(
                        String policyId,
                        Object content) {
        }

        record PolicyCheckResult(
                        boolean passed,
                        String reason) {
        }
}
