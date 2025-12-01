package tech.kayys.wayang.node.core.isolation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.exception.QuotaExceededException;
import tech.kayys.wayang.node.core.model.ExecutionMetrics;
import tech.kayys.wayang.node.core.model.NodeContext;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.model.ResourceProfile;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controls resource quotas for node execution.
 * 
 * Tracks and enforces limits on:
 * - CPU time
 * - Memory usage
 * - Execution duration
 * - Token consumption (for LLM calls)
 * - Cost estimates
 * 
 * Provides per-tenant and per-node quota management.
 */
@ApplicationScoped
public class ResourceQuotaController {
    
    private static final Logger LOG = LoggerFactory.getLogger(ResourceQuotaController.class);
    
    private final MeterRegistry meterRegistry;
    private final boolean quotaEnabled;
    private final Map<String, TenantQuota> tenantQuotas;
    private final Map<String, NodeUsageTracker> nodeTrackers;
    
    @Inject
    public ResourceQuotaController(
        MeterRegistry meterRegistry,
        @ConfigProperty(name = "wayang.quota.enabled", defaultValue = "true")
        boolean quotaEnabled
    ) {
        this.meterRegistry = meterRegistry;
        this.quotaEnabled = quotaEnabled;
        this.tenantQuotas = new ConcurrentHashMap<>();
        this.nodeTrackers = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if execution is within quota limits
     */
    public void checkQuota(NodeDescriptor descriptor, NodeContext context) 
            throws QuotaExceededException {
        
        if (!quotaEnabled) {
            return;
        }
        
        String tenantId = context.getTenantId();
        TenantQuota quota = getTenantQuota(tenantId);
        
        // Check tenant-level quota
        checkTenantQuota(quota, descriptor);
        
        // Check node-level limits
        checkNodeLimits(descriptor, context);
        
        LOG.debug("Quota check passed for tenant {} node {}", 
            tenantId, descriptor.getQualifiedId());
    }
    
    /**
     * Record resource usage after execution
     */
    public void recordUsage(
        NodeDescriptor descriptor,
        NodeContext context,
        ExecutionMetrics metrics
    ) {
        String tenantId = context.getTenantId();
        String nodeId = descriptor.getQualifiedId();
        
        // Update tenant quota
        TenantQuota quota = getTenantQuota(tenantId);
        quota.recordExecution(metrics);
        
        // Update node tracker
        NodeUsageTracker tracker = getNodeTracker(nodeId);
        tracker.recordExecution(metrics);
        
        // Emit metrics
        emitUsageMetrics(tenantId, nodeId, metrics);
        
        LOG.debug("Recorded usage for tenant {} node {}: {} ms, {} bytes", 
            tenantId, nodeId, metrics.duration().toMillis(), metrics.memoryUsedBytes());
    }
    
    /**
     * Get current usage for a tenant
     */
    public TenantUsageSnapshot getTenantUsage(String tenantId) {
        TenantQuota quota = getTenantQuota(tenantId);
        return quota.getSnapshot();
    }
    
    /**
     * Reset tenant quota (for testing or billing cycles)
     */
    public void resetTenantQuota(String tenantId) {
        TenantQuota quota = tenantQuotas.get(tenantId);
        if (quota != null) {
            quota.reset();
            LOG.info("Reset quota for tenant: {}", tenantId);
        }
    }
    
    /**
     * Set custom quota limits for a tenant
     */
    public void setTenantQuota(String tenantId, QuotaLimits limits) {
        TenantQuota quota = new TenantQuota(tenantId, limits);
        tenantQuotas.put(tenantId, quota);
        LOG.info("Set custom quota for tenant {}: {}", tenantId, limits);
    }
    
    /**
     * Get or create tenant quota
     */
    private TenantQuota getTenantQuota(String tenantId) {
        return tenantQuotas.computeIfAbsent(tenantId, 
            id -> new TenantQuota(id, QuotaLimits.defaultLimits()));
    }
    
    /**
     * Get or create node usage tracker
     */
    private NodeUsageTracker getNodeTracker(String nodeId) {
        return nodeTrackers.computeIfAbsent(nodeId, NodeUsageTracker::new);
    }
    
    /**
     * Check tenant-level quota
     */
    private void checkTenantQuota(TenantQuota quota, NodeDescriptor descriptor) 
            throws QuotaExceededException {
        
        if (quota.isExceeded()) {
            meterRegistry.counter("quota.exceeded", 
                "tenant", quota.tenantId,
                "reason", "tenant_limit").increment();
                
            throw new QuotaExceededException(
                "Tenant quota exceeded: " + quota.tenantId,
                quota.getSnapshot()
            );
        }
    }
    
    /**
     * Check node-specific resource limits
     */
    private void checkNodeLimits(NodeDescriptor descriptor, NodeContext context) 
            throws QuotaExceededException {
        
        ResourceProfile profile = descriptor.resourceProfile();
        if (profile == null) {
            return;
        }
        
        // Check timeout
        if (profile.timeout() != null) {
            Instant deadline = context.getCreatedAt().plusSeconds(profile.timeout());
            if (Instant.now().isAfter(deadline)) {
                throw new QuotaExceededException(
                    "Execution deadline exceeded for node: " + descriptor.getQualifiedId()
                );
            }
        }
        
        // Additional limit checks would go here
    }
    
    /**
     * Emit usage metrics
     */
    private void emitUsageMetrics(String tenantId, String nodeId, ExecutionMetrics metrics) {
        Timer.builder("node.execution.duration")
            .tag("tenant", tenantId)
            .tag("node", nodeId)
            .register(meterRegistry)
            .record(metrics.duration());
        
        meterRegistry.counter("node.execution.tokens",
            "tenant", tenantId,
            "node", nodeId).increment(metrics.tokensConsumed());
        
        meterRegistry.gauge("node.execution.cost",
            metrics.costUsd());
    }
    
    /**
     * Tenant quota tracking
     */
    public static class TenantQuota {
        private final String tenantId;
        private final QuotaLimits limits;
        private final AtomicLong executionCount;
        private final AtomicLong totalCpuTimeMs;
        private final AtomicLong totalMemoryBytes;
        private final AtomicLong totalTokens;
        private volatile double totalCostUsd;
        private final Instant resetTime;
        
        public TenantQuota(String tenantId, QuotaLimits limits) {
            this.tenantId = tenantId;
            this.limits = limits;
            this.executionCount = new AtomicLong(0);
            this.totalCpuTimeMs = new AtomicLong(0);
            this.totalMemoryBytes = new AtomicLong(0);
            this.totalTokens = new AtomicLong(0);
            this.totalCostUsd = 0.0;
            this.resetTime = Instant.now();
        }
        
        public void recordExecution(ExecutionMetrics metrics) {
            executionCount.incrementAndGet();
            totalCpuTimeMs.addAndGet(metrics.cpuTimeMillis());
            totalMemoryBytes.addAndGet(metrics.memoryUsedBytes());
            totalTokens.addAndGet(metrics.tokensConsumed());
            
            synchronized (this) {
                totalCostUsd += metrics.costUsd();
            }
        }
        
        public boolean isExceeded() {
            return executionCount.get() >= limits.maxExecutions ||
                   totalCpuTimeMs.get() >= limits.maxCpuTimeMs ||
                   totalMemoryBytes.get() >= limits.maxMemoryBytes ||
                   totalTokens.get() >= limits.maxTokens ||
                   totalCostUsd >= limits.maxCostUsd;
        }
        
        public TenantUsageSnapshot getSnapshot() {
            return new TenantUsageSnapshot(
                tenantId,
                executionCount.get(),
                totalCpuTimeMs.get(),
                totalMemoryBytes.get(),
                totalTokens.get(),
                totalCostUsd,
                limits,
                resetTime
            );
        }
        
        public void reset() {
            executionCount.set(0);
            totalCpuTimeMs.set(0);
            totalMemoryBytes.set(0);
            totalTokens.set(0);
            totalCostUsd = 0.0;
        }
    }
    
    /**
     * Node usage tracking
     */
    private static class NodeUsageTracker {
        private final String nodeId;
        private final AtomicLong executionCount;
        private final AtomicLong totalDurationMs;
        private final AtomicLong failureCount;
        
        public NodeUsageTracker(String nodeId) {
            this.nodeId = nodeId;
            this.executionCount = new AtomicLong(0);
            this.totalDurationMs = new AtomicLong(0);
            this.failureCount = new AtomicLong(0);
        }
        
        public void recordExecution(ExecutionMetrics metrics) {
            executionCount.incrementAndGet();
            totalDurationMs.addAndGet(metrics.duration().toMillis());
        }
        
        public void recordFailure() {
            failureCount.incrementAndGet();
        }
    }
}
