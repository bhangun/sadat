package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DefaultTenantIsolationService - Default implementation of tenant isolation
 * 
 * This implementation provides basic multi-tenancy support with:
 * - Tenant access validation
 * - Resource quota management
 * - Execution limits enforcement
 * - Node permission checks
 * - Timeout configuration per tenant
 * 
 * For production use, this should be extended with:
 * - Database-backed tenant configuration
 * - Dynamic quota updates
 * - Integration with identity/access management systems
 */
@ApplicationScoped
public class DefaultTenantIsolationService implements TenantIsolationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultTenantIsolationService.class);

    // In-memory tenant configurations (should be database-backed in production)
    private final Map<String, TenantConfiguration> tenantConfigs = new ConcurrentHashMap<>();

    // Track resource usage per tenant
    private final Map<String, TenantResourceUsage> resourceUsage = new ConcurrentHashMap<>();

    // Default configuration
    private static final int DEFAULT_MAX_CONCURRENT_RUNS = 10;
    private static final int DEFAULT_MAX_NODES_PER_WORKFLOW = 100;
    private static final long DEFAULT_TIMEOUT_MS = 3600000; // 1 hour
    private static final Set<String> DEFAULT_ALLOWED_NODE_TYPES = Set.of(
            "start", "end", "task", "decision", "parallel", "join",
            "http", "transform", "condition", "loop", "subworkflow");

    @Override
    public Uni<Boolean> validateTenantAccess(String tenantId, WorkflowDefinition workflow) {
        log.debug("Validating tenant access for tenant: {}, workflow: {}", tenantId, workflow.getId());

        return Uni.createFrom().item(() -> {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                log.warn("Invalid tenant ID: {}", tenantId);
                return false;
            }

            // Ensure tenant configuration exists
            getTenantConfigOrDefault(tenantId);

            // Basic validation - in production, check against tenant's workflow permissions
            return true;
        });
    }

    @Override
    public Uni<Boolean> validateTenantResources(String tenantId, WorkflowDefinition workflow) {
        log.debug("Validating tenant resources for tenant: {}, workflow: {}", tenantId, workflow.getId());

        return Uni.createFrom().item(() -> {
            TenantConfiguration config = getTenantConfigOrDefault(tenantId);

            // Check if workflow exceeds node limit
            int nodeCount = workflow.getNodes() != null ? workflow.getNodes().size() : 0;
            int maxNodes = (int) config.getSettings().getOrDefault("maxNodesPerWorkflow",
                    DEFAULT_MAX_NODES_PER_WORKFLOW);

            if (nodeCount > maxNodes) {
                log.warn("Workflow {} exceeds max nodes limit for tenant {}: {} > {}",
                        workflow.getId(), tenantId, nodeCount, maxNodes);
                return false;
            }

            return true;
        });
    }

    @Override
    public Uni<TenantConfiguration> getTenantConfiguration(String tenantId) {
        return Uni.createFrom().item(() -> getTenantConfigOrDefault(tenantId));
    }

    @Override
    public Uni<Boolean> hasQuotaAvailable(String tenantId) {
        return Uni.createFrom().item(() -> {
            TenantResourceUsage usage = resourceUsage.computeIfAbsent(tenantId, k -> new TenantResourceUsage());
            TenantConfiguration config = getTenantConfigOrDefault(tenantId);

            int maxConcurrent = (int) config.getSettings().getOrDefault("maxConcurrentRuns",
                    DEFAULT_MAX_CONCURRENT_RUNS);

            boolean available = usage.activeRuns < maxConcurrent;

            if (!available) {
                log.warn("Quota exceeded for tenant {}: {} active runs (max: {})",
                        tenantId, usage.activeRuns, maxConcurrent);
            }

            return available;
        });
    }

    @Override
    public Uni<Boolean> reserveResources(String tenantId, WorkflowRun run) {
        return Uni.createFrom().item(() -> {
            TenantResourceUsage usage = resourceUsage.computeIfAbsent(tenantId, k -> new TenantResourceUsage());

            synchronized (usage) {
                usage.activeRuns++;
                usage.totalRuns++;
                log.debug("Reserved resources for tenant {}: {} active runs", tenantId, usage.activeRuns);
            }

            return true;
        });
    }

    @Override
    public Uni<Void> releaseResources(String tenantId, String runId) {
        return Uni.createFrom().item(() -> {
            TenantResourceUsage usage = resourceUsage.get(tenantId);

            if (usage != null) {
                synchronized (usage) {
                    usage.activeRuns = Math.max(0, usage.activeRuns - 1);
                    log.debug("Released resources for tenant {}: {} active runs", tenantId, usage.activeRuns);
                }
            }

            return null;
        });
    }

    @Override
    public Uni<ExecutionLimits> getExecutionLimits(String tenantId) {
        return Uni.createFrom().item(() -> {
            TenantConfiguration config = getTenantConfigOrDefault(tenantId);

            ExecutionLimits limits = new ExecutionLimits();
            limits.setMaxConcurrentRuns(
                    (int) config.getSettings().getOrDefault("maxConcurrentRuns", DEFAULT_MAX_CONCURRENT_RUNS));
            limits.setMaxNodesPerWorkflow(
                    (int) config.getSettings().getOrDefault("maxNodesPerWorkflow", DEFAULT_MAX_NODES_PER_WORKFLOW));
            limits.setMaxExecutionTimeMs(
                    (long) config.getSettings().getOrDefault("maxExecutionTimeMs", DEFAULT_TIMEOUT_MS));
            limits.setMaxRetries((int) config.getSettings().getOrDefault("maxRetries", 3));

            return limits;
        });
    }

    @Override
    public Uni<Void> trackUsage(String tenantId, WorkflowRun run) {
        return Uni.createFrom().item(() -> {
            TenantResourceUsage usage = resourceUsage.computeIfAbsent(tenantId, k -> new TenantResourceUsage());

            synchronized (usage) {
                usage.totalExecutionTimeMs += run.getDurationMs() != null ? run.getDurationMs() : 0;
                usage.totalNodesExecuted += run.getNodesExecuted();
            }

            log.debug("Updated usage tracking for tenant {}: {} total runs, {} total nodes",
                    tenantId, usage.totalRuns, usage.totalNodesExecuted);

            return null;
        });
    }

    @Override
    public Uni<List<String>> getNodePermissions(String tenantId) {
        return Uni.createFrom().item(() -> {
            TenantConfiguration config = getTenantConfigOrDefault(tenantId);

            @SuppressWarnings("unchecked")
            Set<String> allowedNodes = (Set<String>) config.getSettings()
                    .getOrDefault("allowedNodeTypes", DEFAULT_ALLOWED_NODE_TYPES);

            return List.copyOf(allowedNodes);
        });
    }

    @Override
    public Uni<Boolean> validateNodeExecution(String tenantId, String nodeType) {
        return getNodePermissions(tenantId)
                .map(allowedNodes -> {
                    boolean allowed = allowedNodes.contains(nodeType);

                    if (!allowed) {
                        log.warn("Node type {} not allowed for tenant {}", nodeType, tenantId);
                    }

                    return allowed;
                });
    }

    @Override
    public Uni<TimeoutConfiguration> getTimeoutConfiguration(String tenantId) {
        return Uni.createFrom().item(() -> {
            TenantConfiguration config = getTenantConfigOrDefault(tenantId);

            TimeoutConfiguration timeout = new TimeoutConfiguration();
            timeout.setWorkflowTimeoutMs(
                    (long) config.getSettings().getOrDefault("maxExecutionTimeMs", DEFAULT_TIMEOUT_MS));
            timeout.setNodeTimeoutMs((long) config.getSettings().getOrDefault("nodeTimeoutMs", 300000L)); // 5 minutes
            timeout.setHumanTaskTimeoutMs((long) config.getSettings().getOrDefault("humanTaskTimeoutMs", 86400000L)); // 24
                                                                                                                      // hours

            return timeout;
        });
    }

    // Helper methods

    private TenantConfiguration getTenantConfigOrDefault(String tenantId) {
        return tenantConfigs.computeIfAbsent(tenantId, k -> {
            TenantConfiguration config = new TenantConfiguration();
            config.setTenantId(tenantId);
            config.setEnvironment("production");
            config.setRegion("default");
            config.setSettings(Map.of(
                    "maxConcurrentRuns", DEFAULT_MAX_CONCURRENT_RUNS,
                    "maxNodesPerWorkflow", DEFAULT_MAX_NODES_PER_WORKFLOW,
                    "maxExecutionTimeMs", DEFAULT_TIMEOUT_MS,
                    "maxRetries", 3,
                    "nodeTimeoutMs", 300000L,
                    "humanTaskTimeoutMs", 86400000L,
                    "allowedNodeTypes", DEFAULT_ALLOWED_NODE_TYPES));

            log.info("Created default configuration for tenant: {}", tenantId);
            return config;
        });
    }

    /**
     * Internal class to track resource usage per tenant
     */
    private static class TenantResourceUsage {
        int activeRuns = 0;
        long totalRuns = 0;
        long totalNodesExecuted = 0;
        long totalExecutionTimeMs = 0;
    }
}
