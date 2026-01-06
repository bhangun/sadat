package tech.kayys.wayang.project.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.project.dto.EndpointConfig;

/**
 * Invokes external endpoints
 */
@ApplicationScoped
public class EndpointInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointInvoker.class);

    public Uni<Object> invoke(EndpointConfig config, Object payload) {
        LOG.info("Invoking endpoint: {} ({})", config.url, config.endpointType);

        return switch (config.endpointType.toLowerCase()) {
            case "rest", "http" -> invokeRest(config, payload);
            case "kafka" -> invokeKafka(config, payload);
            case "database" -> invokeDatabase(config, payload);
            default -> Uni.createFrom().failure(
                    new UnsupportedOperationException(
                            "Endpoint type not supported: " + config.endpointType));
        };
    }

    private Uni<Object> invokeRest(EndpointConfig config, Object payload) {
        // REST API invocation
        return Uni.createFrom().item(payload)
                .onItem().delayIt().by(Duration.ofMillis(100));
    }

    private Uni<Object> invokeKafka(EndpointConfig config, Object payload) {
        // Kafka message publishing
        return Uni.createFrom().item(payload);
    }

    private Uni<Object> invokeDatabase(EndpointConfig config, Object payload) {
        // Database operation
        return Uni.createFrom().item(payload);
    }
}