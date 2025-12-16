package tech.kayys.wayang.collab;

/**
 * CollaborationHealthCheck - Monitor WebSocket connections
 */

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class CollaborationHealthCheck {

    private static final Logger LOG = Logger.getLogger(CollaborationHealthCheck.class);
    private static final long STALE_THRESHOLD_MINUTES = 5;

    /**
     * Clean up stale presence records
     */
    @Scheduled(every = "1m")
    void cleanupStalePresence() {
        Instant threshold = Instant.now().minus(STALE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);

        // Access workflowPresence from CollaborationWebSocket
        // Note: In production, use proper dependency injection
        int cleaned = 0;

        // Implementation would check for stale typing indicators
        // and update presence status to AWAY

        if (cleaned > 0) {
            LOG.infof("Cleaned up %d stale presence records", cleaned);
        }
    }

    /**
     * Monitor active connections
     */
    @Scheduled(every = "30s")
    void monitorConnections() {
        // Log active connections for monitoring
        // In production, emit metrics
    }
}
