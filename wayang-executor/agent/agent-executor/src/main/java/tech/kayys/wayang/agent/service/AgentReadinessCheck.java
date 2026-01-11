package tech.kayys.wayang.agent.service;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.repository.AgentConfigurationRepository;

/**
 * Readiness check for agent system
 */
@ApplicationScoped
public class AgentReadinessCheck implements org.eclipse.microprofile.health.HealthCheck {

    @Inject
    AgentConfigurationRepository configRepo;

    @Override
    @org.eclipse.microprofile.health.Readiness
    public org.eclipse.microprofile.health.HealthCheckResponse call() {
        var builder = org.eclipse.microprofile.health.HealthCheckResponse
                .named("agent-system-ready")
                .up();

        try {
            // Check database connectivity
            configRepo.count()
                    .await().atMost(Duration.ofSeconds(5));

            builder.withData("database", "connected");

            return builder.build();

        } catch (Exception e) {
            return builder.down()
                    .withData("database", "disconnected")
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
