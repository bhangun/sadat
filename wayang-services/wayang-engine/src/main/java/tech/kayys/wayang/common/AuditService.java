package tech.kayys.wayang.common;

/**
 * AuditService - Audit logging service
 */
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuditService {

    private static final Logger LOG = Logger.getLogger(AuditService.class);

    @Inject
    @RestClient
    AuditClient auditClient;

    /**
     * Log audit event (async, fire-and-forget)
     */
    public Uni<Void> log(AuditEvent event) {
        return auditClient.logEvent(event)
                .onFailure().invoke(error -> LOG.errorf(error, "Failed to log audit event: %s", event))
                .replaceWithVoid()
                .onFailure().recoverWithNull();
    }
}