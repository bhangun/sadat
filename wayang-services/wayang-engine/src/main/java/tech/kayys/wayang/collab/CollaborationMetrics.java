package tech.kayys.wayang.collab;

/**
 * CollaborationMetrics - Metrics for collaboration features
 */

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CollaborationMetrics {

    private final Counter connectionsTotal;
    private final Counter messagesTotal;
    private final Counter errorsTotal;

    @Inject
    public CollaborationMetrics(MeterRegistry registry) {
        this.connectionsTotal = Counter.builder("collaboration.connections.total")
                .description("Total WebSocket connections")
                .register(registry);

        this.messagesTotal = Counter.builder("collaboration.messages.total")
                .tag("type", "all")
                .description("Total collaboration messages")
                .register(registry);

        this.errorsTotal = Counter.builder("collaboration.errors.total")
                .description("Total collaboration errors")
                .register(registry);

        // Active sessions gauge
        Gauge.builder("collaboration.sessions.active", () -> getActiveSessionsCount())
                .description("Active collaboration sessions")
                .register(registry);
    }

    public void recordConnection() {
        connectionsTotal.increment();
    }

    public void recordMessage(MessageType type) {
        messagesTotal.increment();
    }

    public void recordError() {
        errorsTotal.increment();
    }

    private double getActiveSessionsCount() {
        // Return count from CollaborationWebSocket
        return 0.0; // Placeholder
    }
}
