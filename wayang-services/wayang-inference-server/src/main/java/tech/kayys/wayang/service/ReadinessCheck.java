package tech.kayys.wayang.service;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import jakarta.inject.Inject;
import tech.kayys.wayang.mcp.CircuitBreaker;
import tech.kayys.wayang.plugin.ModelManager;

class ReadinessCheck implements HealthCheck {
    
    @Inject
    ModelManager modelManager;
    
    @Inject
    MCPIntegrationService mcpService;
    
    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse
            .named("model-readiness");
        
        try {
            var model = modelManager.getActiveModel();
            boolean modelReady = model != null && !model.getCircuitBreaker().getCircuitState()
                .equals(CircuitBreaker.State.OPEN);
            
            builder.status(modelReady)
                .withData("model_ready", modelReady)
                .withData("circuit_breaker", model.getCircuitBreaker().getCircuitState().toString());
            
            // Check MCP
            if (mcpService.getMCPRegistry() != null) {
                var servers = mcpService.getMCPRegistry().getServers();
                long runningServers = servers.values().stream()
                    .filter(s -> s.running())
                    .count();
                
                builder.withData("mcp_servers", servers.size())
                    .withData("mcp_running", runningServers);
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return builder
                .down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}
