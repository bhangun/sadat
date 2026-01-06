package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for health checking cluster nodes and components
 */
@ApplicationScoped
public class HealthCheckService {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckService.class);

    @Inject
    ClusterDiscoveryService clusterDiscoveryService;

    @Inject
    MessageBroker messageBroker;

    private final Map<String, NodeHealth> nodeHealthStatus = new ConcurrentHashMap<>();
    private final Map<String, ComponentHealth> componentHealthStatus = new ConcurrentHashMap<>();
    private final ScheduledExecutorService healthCheckExecutor;
    private volatile boolean isRunning = false;

    public HealthCheckService() {
        this.healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-check-executor");
            t.setDaemon(true);
            return t;
        });
    }

    public Uni<Map<String, Object>> checkClusterHealth() {
        return clusterDiscoveryService.getClusterMembers()
                .flatMap(members -> {
                    List<Uni<NodeHealth>> healthChecks = members.stream()
                            .map(member -> checkNodeHealth(member.getId()))
                            .toList();

                    return Uni.combine().all().unis(healthChecks).asList()
                            .map(healthResults -> {
                                Map<String, Object> clusterHealth = new HashMap<>();

                                int total = members.size();
                                int healthy = 0;
                                int degraded = 0;
                                int unhealthy = 0;

                                Map<String, Map<String, Object>> nodeDetails = new HashMap<>();

                                for (int i = 0; i < members.size(); i++) {
                                    ClusterNode member = members.get(i);
                                    NodeHealth health = healthResults.get(i);

                                    nodeDetails.put(member.getId(), health.toMap());

                                    switch (health.getStatus()) {
                                        case HEALTHY -> healthy++;
                                        case DEGRADED -> degraded++;
                                        case UNHEALTHY -> unhealthy++;
                                    }
                                }

                                clusterHealth.put("totalNodes", total);
                                clusterHealth.put("healthyNodes", healthy);
                                clusterHealth.put("degradedNodes", degraded);
                                clusterHealth.put("unhealthyNodes", unhealthy);
                                clusterHealth.put("nodeDetails", nodeDetails);
                                clusterHealth.put("overallStatus",
                                        calculateOverallStatus(healthy, degraded, unhealthy, total));
                                clusterHealth.put("timestamp", Instant.now().toString());

                                return clusterHealth;
                            });
                });
    }

    public Uni<NodeHealth> checkNodeHealth(String nodeId) {
        return Uni.createFrom().deferred(() -> {
            NodeHealth cached = nodeHealthStatus.get(nodeId);
            Instant now = Instant.now();

            // Return cached result if recent (within 30 seconds)
            if (cached != null && cached.getLastChecked().plusSeconds(30).isAfter(now)) {
                return Uni.createFrom().item(cached);
            }

            // Perform fresh health check
            return performNodeHealthCheck(nodeId)
                    .onItem().invoke(health -> {
                        nodeHealthStatus.put(nodeId, health);
                        LOG.debug("Health check completed for node {}: {}", nodeId, health.getStatus());
                    })
                    .onFailure().recoverWithItem(th -> {
                        NodeHealth unhealthy = NodeHealth.unhealthy(nodeId,
                                "Health check failed: " + th.getMessage());
                        nodeHealthStatus.put(nodeId, unhealthy);
                        return unhealthy;
                    });
        });
    }

    public Uni<ComponentHealth> checkComponentHealth(String componentName) {
        return Uni.createFrom().deferred(() -> {
            ComponentHealth cached = componentHealthStatus.get(componentName);
            Instant now = Instant.now();

            if (cached != null && cached.getLastChecked().plusSeconds(30).isAfter(now)) {
                return Uni.createFrom().item(cached);
            }

            return performComponentHealthCheck(componentName)
                    .onItem().invoke(health -> {
                        componentHealthStatus.put(componentName, health);
                        LOG.debug("Health check completed for component {}: {}",
                                componentName, health.getStatus());
                    })
                    .onFailure().recoverWithItem(th -> {
                        ComponentHealth unhealthy = ComponentHealth.unhealthy(componentName,
                                "Health check failed: " + th.getMessage());
                        componentHealthStatus.put(componentName, unhealthy);
                        return unhealthy;
                    });
        });
    }

    public Uni<Map<String, ComponentHealth>> checkAllComponents() {
        return Uni.createFrom().deferred(() -> {
            List<String> components = List.of(
                    "WorkflowEngine",
                    "WorkflowRunManager",
                    "EventStore",
                    "ClusterCoordinator",
                    "MessageBroker",
                    "Database");

            List<Uni<ComponentHealth>> checks = components.stream()
                    .map(this::checkComponentHealth)
                    .toList();

            return Uni.combine().all().unis(checks).asList()
                    .map(healthResults -> {
                        Map<String, ComponentHealth> results = new HashMap<>();
                        for (int i = 0; i < components.size(); i++) {
                            results.put(components.get(i), healthResults.get(i));
                        }
                        return Map.copyOf(results);
                    });
        });
    }

    public Uni<Void> registerHealthIndicator(String componentName, HealthIndicator indicator) {
        return Uni.createFrom().deferred(() -> {
            // In a real implementation, would register the indicator
            LOG.info("Registered health indicator for component: {}", componentName);
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> startPeriodicHealthChecks() {
        return Uni.createFrom().deferred(() -> {
            if (isRunning) {
                return Uni.createFrom().voidItem();
            }

            isRunning = true;
            healthCheckExecutor.scheduleAtFixedRate(() -> {
                try {
                    runHealthChecks();
                } catch (Exception e) {
                    LOG.error("Error in periodic health checks", e);
                }
            }, 0, 30, TimeUnit.SECONDS);

            LOG.info("Periodic health checks started");
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> stopPeriodicHealthChecks() {
        return Uni.createFrom().deferred(() -> {
            isRunning = false;
            healthCheckExecutor.shutdown();
            LOG.info("Periodic health checks stopped");
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<HealthSummary> getHealthSummary() {
        return checkClusterHealth()
                .flatMap(clusterHealth -> checkAllComponents()
                        .map(components -> {
                            int totalComponents = components.size();
                            long healthyComponents = components.values().stream()
                                    .filter(h -> h.getStatus() == HealthStatus.HEALTHY)
                                    .count();

                            return new HealthSummary(
                                    (String) clusterHealth.get("overallStatus"),
                                    clusterHealth,
                                    components,
                                    Instant.now());
                        }));
    }

    private Uni<NodeHealth> performNodeHealthCheck(String nodeId) {
        return Uni.createFrom().deferred(() -> {
            List<HealthCheck> checks = new ArrayList<>();

            // Check 1: Node reachability via ping
            checks.add(checkNodeReachability(nodeId));

            // Check 2: Message broker connectivity
            checks.add(checkMessageBrokerConnectivity(nodeId));

            // Check 3: Resource usage (simulated)
            checks.add(checkNodeResources(nodeId));

            return Uni.combine().all().unis(
                    checks.stream().map(HealthCheck::check).toList()).asList()
                    .map(checkResults -> {
                        List<String> errors = new ArrayList<>();
                        List<String> warnings = new ArrayList<>();
                        boolean allHealthy = true;

                        for (HealthCheck.Result result : checkResults) {
                            if (!result.isHealthy()) {
                                allHealthy = false;
                                errors.add(result.getMessage());
                            } else if (result.hasWarning()) {
                                warnings.add(result.getWarning());
                            }
                        }

                        HealthStatus status;
                        if (allHealthy && warnings.isEmpty()) {
                            status = HealthStatus.HEALTHY;
                        } else if (allHealthy && !warnings.isEmpty()) {
                            status = HealthStatus.DEGRADED;
                        } else {
                            status = HealthStatus.UNHEALTHY;
                        }

                        return new NodeHealth(
                                nodeId,
                                status,
                                errors,
                                warnings,
                                Instant.now(),
                                checkResults.stream()
                                        .collect(HashMap::new,
                                                (map, result) -> map.put(result.getCheckName(), result.toMap()),
                                                HashMap::putAll));
                    });
        });
    }

    private Uni<ComponentHealth> performComponentHealthCheck(String componentName) {
        return Uni.createFrom().deferred(() -> {
            // Simulated component checks
            // In real implementation, would call component-specific health endpoints

            try {
                // Simulate different health statuses based on component
                HealthStatus status;
                List<String> issues = new ArrayList<>();

                switch (componentName) {
                    case "Database":
                        status = Math.random() > 0.1 ? HealthStatus.HEALTHY : HealthStatus.DEGRADED;
                        if (status == HealthStatus.DEGRADED) {
                            issues.add("High latency on queries");
                        }
                        break;
                    case "MessageBroker":
                        status = Math.random() > 0.05 ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
                        if (status == HealthStatus.UNHEALTHY) {
                            issues.add("Connection timeout");
                        }
                        break;
                    default:
                        status = HealthStatus.HEALTHY;
                }

                return Uni.createFrom().item(new ComponentHealth(
                        componentName,
                        status,
                        issues,
                        Instant.now(),
                        Map.of("responseTime", Math.random() * 100)));
            } catch (Exception e) {
                return Uni.createFrom().item(ComponentHealth.unhealthy(
                        componentName, "Check failed: " + e.getMessage()));
            }
        });
    }

    private HealthCheck checkNodeReachability(String nodeId) {
        return new HealthCheck("reachability", () -> {
            // Simulate network ping
            boolean reachable = Math.random() > 0.05; // 95% success rate

            if (reachable) {
                return HealthCheck.Result.healthy("Node is reachable");
            } else {
                return HealthCheck.Result.unhealthy("Node is unreachable");
            }
        });
    }

    private HealthCheck checkMessageBrokerConnectivity(String nodeId) {
        return new HealthCheck("message-broker", () -> {
            // Check if node can send/receive messages
            boolean connected = Math.random() > 0.02; // 98% success rate

            if (connected) {
                double latency = Math.random() * 50; // 0-50ms
                if (latency > 30) {
                    return HealthCheck.Result.healthyWithWarning(
                            "Message broker connected",
                            String.format("High latency: %.2fms", latency));
                }
                return HealthCheck.Result.healthy("Message broker connected");
            } else {
                return HealthCheck.Result.unhealthy("Message broker disconnected");
            }
        });
    }

    private HealthCheck checkNodeResources(String nodeId) {
        return new HealthCheck("resources", () -> {
            // Simulate resource checks
            double cpuUsage = Math.random() * 100;
            double memoryUsage = Math.random() * 100;

            List<String> warnings = new ArrayList<>();

            if (cpuUsage > 80) {
                warnings.add(String.format("High CPU usage: %.1f%%", cpuUsage));
            }
            if (memoryUsage > 85) {
                warnings.add(String.format("High memory usage: %.1f%%", memoryUsage));
            }

            if (!warnings.isEmpty()) {
                return HealthCheck.Result.healthyWithWarning(
                        "Resources within limits",
                        String.join("; ", warnings));
            }

            return HealthCheck.Result.healthy("Resources OK");
        });
    }

    private void runHealthChecks() {
        LOG.debug("Running periodic health checks");

        clusterDiscoveryService.getClusterMembers()
                .onItem().transformToMulti(members -> Multi.createFrom().iterable(members))
                .onItem().transformToUniAndMerge(member -> checkNodeHealth(member.getId())
                        .onFailure().recoverWithNull())
                .collect().asList()
                .subscribe().with(
                        results -> LOG.trace("Periodic health checks completed for {} nodes", results.size()),
                        failure -> LOG.error("Periodic health checks failed", failure));
    }

    private String calculateOverallStatus(int healthy, int degraded, int unhealthy, int total) {
        if (total == 0)
            return "UNKNOWN";
        if (unhealthy > 0)
            return "UNHEALTHY";
        if (degraded > 0)
            return "DEGRADED";
        return "HEALTHY";
    }

    public enum HealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
    }

    public static class NodeHealth {
        private final String nodeId;
        private final HealthStatus status;
        private final List<String> errors;
        private final List<String> warnings;
        private final Instant lastChecked;
        private final Map<String, Map<String, Object>> checkDetails;

        public NodeHealth(String nodeId, HealthStatus status, List<String> errors,
                List<String> warnings, Instant lastChecked,
                Map<String, Map<String, Object>> checkDetails) {
            this.nodeId = nodeId;
            this.status = status;
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
            this.lastChecked = lastChecked;
            this.checkDetails = Map.copyOf(checkDetails);
        }

        public static NodeHealth healthy(String nodeId) {
            return new NodeHealth(nodeId, HealthStatus.HEALTHY, List.of(), List.of(),
                    Instant.now(), Map.of());
        }

        public static NodeHealth unhealthy(String nodeId, String error) {
            return new NodeHealth(nodeId, HealthStatus.UNHEALTHY, List.of(error),
                    List.of(), Instant.now(), Map.of());
        }

        // Getters...
        public String getNodeId() {
            return nodeId;
        }

        public HealthStatus getStatus() {
            return status;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public Instant getLastChecked() {
            return lastChecked;
        }

        public Map<String, Map<String, Object>> getCheckDetails() {
            return checkDetails;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("nodeId", nodeId);
            map.put("status", status.name());
            map.put("errors", errors);
            map.put("warnings", warnings);
            map.put("lastChecked", lastChecked.toString());
            map.put("checkDetails", checkDetails);
            return map;
        }
    }

    public static class ComponentHealth {
        private final String componentName;
        private final HealthStatus status;
        private final List<String> issues;
        private final Instant lastChecked;
        private final Map<String, Object> metrics;

        public ComponentHealth(String componentName, HealthStatus status,
                List<String> issues, Instant lastChecked,
                Map<String, Object> metrics) {
            this.componentName = componentName;
            this.status = status;
            this.issues = List.copyOf(issues);
            this.lastChecked = lastChecked;
            this.metrics = Map.copyOf(metrics);
        }

        public static ComponentHealth healthy(String componentName) {
            return new ComponentHealth(componentName, HealthStatus.HEALTHY,
                    List.of(), Instant.now(), Map.of());
        }

        public static ComponentHealth unhealthy(String componentName, String issue) {
            return new ComponentHealth(componentName, HealthStatus.UNHEALTHY,
                    List.of(issue), Instant.now(), Map.of());
        }

        // Getters...
        public String getComponentName() {
            return componentName;
        }

        public HealthStatus getStatus() {
            return status;
        }

        public List<String> getIssues() {
            return issues;
        }

        public Instant getLastChecked() {
            return lastChecked;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }
    }

    public static class HealthSummary {
        private final String overallStatus;
        private final Map<String, Object> clusterHealth;
        private final Map<String, ComponentHealth> componentHealth;
        private final Instant timestamp;

        public HealthSummary(String overallStatus, Map<String, Object> clusterHealth,
                Map<String, ComponentHealth> componentHealth, Instant timestamp) {
            this.overallStatus = overallStatus;
            this.clusterHealth = Map.copyOf(clusterHealth);
            this.componentHealth = Map.copyOf(componentHealth);
            this.timestamp = timestamp;
        }

        // Getters...
    }

    @FunctionalInterface
    public interface HealthIndicator {
        Uni<HealthStatus> check();
    }

    @FunctionalInterface
    private interface HealthCheckFunction {
        HealthCheck.Result check();
    }

    private static class HealthCheck {
        private final String name;
        private final HealthCheckFunction checkFunction;

        public HealthCheck(String name, HealthCheckFunction checkFunction) {
            this.name = name;
            this.checkFunction = checkFunction;
        }

        public Uni<Result> check() {
            return Uni.createFrom().deferred(() -> {
                try {
                    Result result = checkFunction.check();
                    result.setCheckName(name);
                    return Uni.createFrom().item(result);
                } catch (Exception e) {
                    return Uni.createFrom().item(Result.unhealthy(
                            "Check failed with exception: " + e.getMessage()));
                }
            });
        }

        public static class Result {
            private final boolean healthy;
            private final String message;
            private final String warning;
            private final Map<String, Object> details;
            private String checkName;

            private Result(boolean healthy, String message, String warning,
                    Map<String, Object> details) {
                this.healthy = healthy;
                this.message = message;
                this.warning = warning;
                this.details = Map.copyOf(details);
            }

            public static Result healthy(String message) {
                return new Result(true, message, null, Map.of());
            }

            public static Result healthyWithWarning(String message, String warning) {
                return new Result(true, message, warning, Map.of());
            }

            public static Result unhealthy(String message) {
                return new Result(false, message, null, Map.of());
            }

            public boolean isHealthy() {
                return healthy;
            }

            public String getMessage() {
                return message;
            }

            public String getWarning() {
                return warning;
            }

            public boolean hasWarning() {
                return warning != null;
            }

            public Map<String, Object> getDetails() {
                return details;
            }

            public String getCheckName() {
                return checkName;
            }

            public void setCheckName(String checkName) {
                this.checkName = checkName;
            }

            public Map<String, Object> toMap() {
                Map<String, Object> map = new HashMap<>();
                map.put("healthy", healthy);
                map.put("message", message);
                if (warning != null)
                    map.put("warning", warning);
                map.put("details", details);
                return map;
            }
        }
    }
}