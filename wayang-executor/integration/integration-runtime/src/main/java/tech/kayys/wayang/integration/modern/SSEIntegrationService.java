package tech.kayys.silat.executor.camel.modern;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Server-Sent Events integration
 */
@ApplicationScoped
public class SSEIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(SSEIntegrationService.class);

    /**
     * Subscribe to SSE endpoint
     */
    public Multi<SSEEvent> subscribe(String sseUrl, String tenantId) {

        return Multi.createFrom().emitter(emitter -> {
            try {
                // Use Camel's built-in SSE support
                String routeId = "sse-subscribe-" + UUID.randomUUID();

                org.apache.camel.CamelContext context = new org.apache.camel.impl.DefaultCamelContext();

                context.addRoutes(new org.apache.camel.builder.RouteBuilder() {
                    @Override
                    public void configure() {
                        from("ahc-sse:" + sseUrl)
                            .routeId(routeId)
                            .process(exchange -> {
                                String eventData = exchange.getIn().getBody(String.class);
                                String eventId = exchange.getIn().getHeader(
                                    "CamelAhcSseEventId", String.class
                                );
                                String eventType = exchange.getIn().getHeader(
                                    "CamelAhcSseEventType", String.class
                                );

                                SSEEvent event = new SSEEvent(
                                    eventId,
                                    eventType != null ? eventType : "message",
                                    eventData,
                                    tenantId,
                                    Instant.now()
                                );

                                emitter.emit(event);
                            });
                    }
                });

                context.start();

                emitter.onTermination(() -> {
                    try {
                        context.stop();
                    } catch (Exception e) {
                        LOG.error("Error stopping SSE subscription", e);
                    }
                });

            } catch (Exception e) {
                LOG.error("SSE subscription failed", e);
                emitter.fail(e);
            }
        });
    }
}