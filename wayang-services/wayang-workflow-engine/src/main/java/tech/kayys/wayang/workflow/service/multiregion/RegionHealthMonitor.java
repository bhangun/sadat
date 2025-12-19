package tech.kayys.wayang.workflow.service.multiregion;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.service.multiregion.RegionHealthStatus.HealthState;

/**
 * Monitors health of all available regions
 */
@ApplicationScoped
public class RegionHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(RegionHealthMonitor.class);

    @ConfigProperty(name = "app.regions.health.check.interval.seconds", defaultValue = "30")
    long healthCheckIntervalSeconds;

    @ConfigProperty(name = "app.regions.health.timeout.millis", defaultValue = "5000")
    long healthCheckTimeoutMillis;

    @ConfigProperty(name = "app.regions.health.error.threshold", defaultValue = "0.1")
    double errorThreshold;

    @ConfigProperty(name = "app.regions.health.latency.threshold.millis", defaultValue = "1000")
    double latencyThresholdMillis;

    @Inject
    HealthCheckClient healthCheckClient;

    @Inject
    @Any
    Instance<HealthCheckStrategy> healthCheckStrategies;

    private final Map<String, RegionHealthStatus> regionHealthCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean monitoringActive = false;

    @PostConstruct
    void initialize() {
        startHealthMonitoring();
    }

    @PreDestroy
    void shutdown() {
        stopHealthMonitoring();
    }

    /**
     * Get list of currently healthy regions
     */
    public Uni<List<String>> getHealthyRegions() {
        return getAllRegionStatus()
                .onItem().transform(statusMap -> statusMap.entrySet().stream()
                        .filter(entry -> entry.getValue().isHealthy())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()));
    }

    /**
     * Check health of a specific region
     */
    public Uni<Boolean> checkRegionHealth(String regionId) {
        return performHealthCheck(regionId)
                .onItem().transform(RegionHealthStatus::isHealthy)
                .onFailure().recoverWithItem(ex -> {
                    log.warn("Health check failed for region: {}", regionId, ex);
                    return false;
                });
    }

    /**
     * Get comprehensive health status for all regions
     */
    public Uni<Map<String, RegionHealthStatus>> getAllRegionStatus() {
        return Uni.createFrom().item(() -> Collections.unmodifiableMap(new HashMap<>(regionHealthCache)));
    }

    /**
     * Get health status for a specific region
     */
    public Uni<Optional<RegionHealthStatus>> getRegionStatus(String regionId) {
        return Uni.createFrom().item(() -> Optional.ofNullable(regionHealthCache.get(regionId)));
    }

    /**
     * Force immediate health check for a region
     */
    public Uni<RegionHealthStatus> forceHealthCheck(String regionId) {
        return performHealthCheck(regionId)
                .onItem().invoke(status -> {
                    regionHealthCache.put(regionId, status);
                    log.debug("Health check completed for region: {}, status: {}",
                            regionId, status.state());
                });
    }

    // Private implementation methods

    private void startHealthMonitoring() {
        if (!monitoringActive) {
            monitoringActive = true;
            scheduler.scheduleAtFixedRate(
                    this::performBackgroundHealthChecks,
                    0,
                    healthCheckIntervalSeconds,
                    TimeUnit.SECONDS);
            log.info("Region health monitoring started with interval: {}s",
                    healthCheckIntervalSeconds);
        }
    }

    private void stopHealthMonitoring() {
        monitoringActive = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Region health monitoring stopped");
    }

    private void performBackgroundHealthChecks() {
        if (!monitoringActive) {
            return;
        }

        try {
            // In a real implementation, this would check all regions asynchronously
            log.debug("Background health checks running");
        } catch (Exception e) {
            log.error("Background health check failed", e);
        }
    }

    private Uni<RegionHealthStatus> performHealthCheck(String regionId) {
        return healthCheckClient.checkHealth(regionId)
                .onItem().transform(response -> evaluateHealthStatus(regionId, response))
                .onFailure().recoverWithUni(ex -> Uni.createFrom().item(createUnhealthyStatus(regionId, ex)));
    }

    private RegionHealthStatus evaluateHealthStatus(String regionId, HealthCheckResponse response) {
        HealthState state = determineHealthState(response);

        return RegionHealthStatus.builder(regionId)
                .state(state)
                .lastChecked(Instant.now())
                .latencyMillis(response.latencyMillis())
                .errorRate(response.errorRate())
                .activeConnections(response.activeConnections())
                .metrics(response.metrics())
                .diagnosticMessage(response.diagnosticMessage())
                .build();
    }

    private HealthState determineHealthState(HealthCheckResponse response) {
        if (response.errorRate() > errorThreshold) {
            return HealthState.UNHEALTHY;
        }

        if (response.latencyMillis() > latencyThresholdMillis) {
            return HealthState.DEGRADED;
        }

        return response.success() ? HealthState.HEALTHY : HealthState.UNHEALTHY;
    }

    private RegionHealthStatus createUnhealthyStatus(String regionId, Throwable error) {
        return RegionHealthStatus.builder(regionId)
                .state(HealthState.UNHEALTHY)
                .lastChecked(Instant.now())
                .errorRate(1.0)
                .diagnosticMessage(Optional.of("Health check failed: " + error.getMessage()))
                .build();
    }
}
