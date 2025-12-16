package tech.kayys.wayang.common;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * AuditClient - REST client for audit service
 */
@RegisterRestClient(configKey = "audit-service")
@Path("/api/v1/audit")
@Produces(MediaType.APPLICATION_JSON)
public interface AuditClient {

    @POST
    @Path("/events")
    @Retry(maxRetries = 2, delay = 100)
    @Timeout(value = 5000)
    Uni<Void> logEvent(AuditEvent event);
}