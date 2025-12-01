package tech.kayys.wayang.service;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.ModelManager;

class StartupCheck implements HealthCheck {
    
    @Inject
    ModelManager modelManager;
    
    private volatile boolean started = false;
    
    public void markStarted() {
        this.started = true;
    }
    
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse
            .named("startup")
            .status(started)
            .withData("started", started)
            .build();
    }
}
