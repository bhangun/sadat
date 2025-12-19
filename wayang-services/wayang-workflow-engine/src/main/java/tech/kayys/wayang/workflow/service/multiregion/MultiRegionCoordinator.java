package tech.kayys.wayang.workflow.service.multiregion;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.api.model.WorkflowEvent;

/**
 * Multi-Region Active-Active Architecture
 */
/**
 * Multi-Region Coordinator with configurable strategies and health monitoring
 */
@ApplicationScoped
public class MultiRegionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(MultiRegionCoordinator.class);

    @Inject
    @ConfigProperty(name = "app.region.current")
    String currentRegion;

    @ConfigProperty(name = "app.regions.available", defaultValue = "us-east-1,us-west-2,eu-west-1")
    List<String> availableRegions;

    @ConfigProperty(name = "app.regions.prioritize-low-latency", defaultValue = "true")
    boolean prioritizeLowLatency;

    @ConfigProperty(name = "app.regions.replication.enabled", defaultValue = "false")
    boolean replicationEnabled;

    @Inject
    @Any
    Instance<RegionSelectionStrategy> selectionStrategies;

    @Inject
    RegionHealthMonitor healthMonitor;

    @Inject
    EventReplicationService replicationService;

    private RegionSelectionStrategy activeStrategy;
    private final Map<String, RegionMetadata> regionMetadata = new ConcurrentHashMap<>();

    @PostConstruct
    void initialize() {
        loadRegionMetadata();
        determineActiveStrategy();
        log.info("MultiRegionCoordinator initialized for region: {}", currentRegion);
    }

    /**
     * Determine optimal region for workflow execution with fallback logic
     */
    public Uni<String> selectOptimalRegion(String tenantId, Optional<Map<String, Object>> context) {
        return healthMonitor.getHealthyRegions()
                .onItem().transform(healthyRegions -> {
                    if (healthyRegions.isEmpty()) {
                        log.warn("No healthy regions available, using current region");
                        return currentRegion;
                    }

                    return activeStrategy.selectRegion(tenantId, healthyRegions, context);
                })
                .onFailure().recoverWithItem(ex -> {
                    log.error("Region selection failed, using current region", ex);
                    return currentRegion;
                });
    }

    /**
     * Intelligent region selection with tenant affinity support
     */
    public Uni<String> selectRegion(String tenantId) {
        return selectOptimalRegion(tenantId, Optional.empty());
    }

    /**
     * Cross-region failover with validation and state transition
     */
    public Uni<FailoverResult> initiateFailover(String targetRegion, FailoverReason reason) {
        return validateFailoverTarget(targetRegion)
                .chain(valid -> {
                    if (!valid) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Invalid failover target: " + targetRegion));
                    }

                    log.info("Initiating failover to region: {}, reason: {}", targetRegion, reason);

                    return healthMonitor.checkRegionHealth(targetRegion)
                            .chain(isHealthy -> {
                                if (!isHealthy) {
                                    return Uni.createFrom().failure(
                                            new IllegalStateException("Target region is unhealthy: " + targetRegion));
                                }

                                return performFailover(targetRegion, reason);
                            });
                })
                .onFailure().recoverWithUni(ex -> {
                    log.error("Failover failed for region: {}", targetRegion, ex);
                    return Uni.createFrom().item(FailoverResult.failed(ex.getMessage()));
                });
    }

    /**
     * Cross-region event replication with configurable consistency
     */
    public Uni<ReplicationResult> replicateEvent(WorkflowEvent event, ReplicationMode mode) {
        if (!replicationEnabled) {
            log.debug("Replication disabled, skipping event: {}", event.type());
            return Uni.createFrom().item(ReplicationResult.disabled());
        }

        return replicationService.replicate(event, mode)
                .onItem().invoke(result -> log.debug("Event replicated: {}, regions: {}, mode: {}",
                        event.type(), result.replicatedRegions(), mode))
                .onFailure().recoverWithUni(ex -> {
                    log.warn("Event replication failed: {}", event.type(), ex);
                    return Uni.createFrom().item(ReplicationResult.failed(ex.getMessage()));
                });
    }

    /**
     * Overload for backward compatibility
     */
    public Uni<Void> replicateEvent(WorkflowEvent event) {
        return replicateEvent(event, ReplicationMode.ASYNC)
                .onItem().ignore().andContinueWithNull();
    }

    /**
     * Get region metadata for monitoring/observability
     */
    public Uni<Map<String, RegionMetadata>> getRegionStatus() {
        return healthMonitor.getAllRegionStatus()
                .onItem().transform(statusMap -> {
                    Map<String, RegionMetadata> result = new HashMap<>(regionMetadata);
                    statusMap.forEach(
                            (region, status) -> result.computeIfPresent(region, (k, v) -> v.withHealthStatus(status)));
                    return Collections.unmodifiableMap(result);
                });
    }

    // Private implementation methods

    private void loadRegionMetadata() {
        availableRegions.forEach(region -> regionMetadata.put(region, RegionMetadata.builder()
                .regionId(region)
                .endpoints(Map.of())
                .capabilities(Set.of())
                .build()));
    }

    private void determineActiveStrategy() {
        if (prioritizeLowLatency) {
            activeStrategy = new LatencyBasedStrategy();
        } else {
            activeStrategy = new RoundRobinStrategy();
        }

        // Could also use CDI to inject specific strategy
        log.debug("Using region selection strategy: {}", activeStrategy.getClass().getSimpleName());
    }

    private Uni<Boolean> validateFailoverTarget(String targetRegion) {
        return Uni.createFrom()
                .item(() -> availableRegions.contains(targetRegion) && !targetRegion.equals(currentRegion));
    }

    private Uni<FailoverResult> performFailover(String targetRegion, FailoverReason reason) {
        // Implementation would coordinate with other services
        // For now, simulate async operation
        return Uni.createFrom().item(() -> FailoverResult.success(targetRegion, Instant.now(), reason));
    }

    // Supporting record classes

    public record RegionMetadata(
            String regionId,
            Map<String, String> endpoints,
            Set<String> capabilities,
            Optional<RegionHealthStatus> healthStatus) {
        public RegionMetadata withHealthStatus(RegionHealthStatus status) {
            return new RegionMetadata(regionId, endpoints, capabilities, Optional.of(status));
        }

        static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String regionId;
            private Map<String, String> endpoints = new HashMap<>();
            private Set<String> capabilities = new HashSet<>();

            Builder regionId(String regionId) {
                this.regionId = regionId;
                return this;
            }

            Builder endpoints(Map<String, String> endpoints) {
                this.endpoints = endpoints;
                return this;
            }

            Builder capabilities(Set<String> capabilities) {
                this.capabilities = capabilities;
                return this;
            }

            RegionMetadata build() {
                return new RegionMetadata(regionId,
                        Collections.unmodifiableMap(endpoints),
                        Collections.unmodifiableSet(capabilities),
                        Optional.empty());
            }
        }
    }

    public enum FailoverReason {
        DISASTER_RECOVERY,
        PERFORMANCE_DEGRADATION,
        SCHEDULED_MAINTENANCE,
        MANUAL_INTERVENTION
    }

    public enum ReplicationMode {
        SYNC, // Synchronous replication
        ASYNC, // Asynchronous replication
        BATCH, // Batched replication
        LAZY // On-demand replication
    }

    public record FailoverResult(
            String targetRegion,
            Instant timestamp,
            FailoverReason reason,
            boolean success,
            Optional<String> errorMessage) {
        public static FailoverResult success(String region, Instant time, FailoverReason reason) {
            return new FailoverResult(region, time, reason, true, Optional.empty());
        }

        public static FailoverResult failed(String error) {
            return new FailoverResult(null, Instant.now(), null, false, Optional.of(error));
        }
    }

    public record ReplicationResult(
            String eventId,
            Set<String> replicatedRegions,
            ReplicationMode mode,
            boolean successful,
            Optional<String> errorMessage) {
        public static ReplicationResult disabled() {
            return new ReplicationResult(null, Set.of(), null, true, Optional.of("Replication disabled"));
        }

        public static ReplicationResult failed(String error) {
            return new ReplicationResult(null, Set.of(), null, false, Optional.of(error));
        }
    }
}
