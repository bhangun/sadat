package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

import java.util.List;
import java.util.Map;

/**
 * TenantIsolationService - Interface for handling multi-tenancy and resource
 * isolation
 * 
 * This interface provides a contract for tenant-specific operations, resource
 * management,
 * and isolation mechanisms, making the workflow engine suitable for
 * multi-tenant use cases.
 */
public interface TenantIsolationService {

    /**
     * Validate tenant access for workflow execution
     * 
     * @param tenantId The tenant ID
     * @param workflow The workflow to validate
     * @return A Uni indicating whether access is allowed
     */
    Uni<Boolean> validateTenantAccess(String tenantId, WorkflowDefinition workflow);

    /**
     * Validate tenant resources for workflow execution
     * 
     * @param tenantId The tenant ID
     * @param workflow The workflow to validate
     * @return A Uni indicating whether resources are available
     */
    Uni<Boolean> validateTenantResources(String tenantId, WorkflowDefinition workflow);

    /**
     * Get tenant-specific configuration
     * 
     * @param tenantId The tenant ID
     * @return A Uni containing tenant configuration
     */
    Uni<TenantConfiguration> getTenantConfiguration(String tenantId);

    /**
     * Check if tenant has quota available for a new run
     * 
     * @param tenantId The tenant ID
     * @return A Uni indicating whether quota is available
     */
    Uni<Boolean> hasQuotaAvailable(String tenantId);

    /**
     * Reserve resources for a workflow run
     * 
     * @param tenantId The tenant ID
     * @param run      The workflow run
     * @return A Uni indicating whether resources were successfully reserved
     */
    Uni<Boolean> reserveResources(String tenantId, WorkflowRun run);

    /**
     * Release resources for a completed workflow run
     * 
     * @param tenantId The tenant ID
     * @param runId    The run ID
     * @return A Uni indicating completion
     */
    Uni<Void> releaseResources(String tenantId, String runId);

    /**
     * Get tenant-specific execution limits
     * 
     * @param tenantId The tenant ID
     * @return A Uni containing execution limits
     */
    Uni<ExecutionLimits> getExecutionLimits(String tenantId);

    /**
     * Track tenant usage
     * 
     * @param tenantId The tenant ID
     * @param run      The workflow run
     * @return A Uni indicating completion
     */
    Uni<Void> trackUsage(String tenantId, WorkflowRun run);

    /**
     * Get tenant-specific node permissions
     * 
     * @param tenantId The tenant ID
     * @return A Uni containing allowed node types
     */
    Uni<List<String>> getNodePermissions(String tenantId);

    /**
     * Validate node execution for tenant
     * 
     * @param tenantId The tenant ID
     * @param nodeType The node type to validate
     * @return A Uni indicating whether the node type is allowed
     */
    Uni<Boolean> validateNodeExecution(String tenantId, String nodeType);

    /**
     * Get tenant-specific timeout configuration
     * 
     * @param tenantId The tenant ID
     * @return A Uni containing timeout configuration
     */
    Uni<TimeoutConfiguration> getTimeoutConfiguration(String tenantId);
}

/**
 * TenantConfiguration - Configuration for a specific tenant
 */
class TenantConfiguration {
    private String tenantId;
    private Map<String, Object> settings;
    private String environment;
    private String region;

    public TenantConfiguration() {
    }

    public TenantConfiguration(String tenantId, Map<String, Object> settings, String environment, String region) {
        this.tenantId = tenantId;
        this.settings = settings;
        this.environment = environment;
        this.region = region;
    }

    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}

/**
 * ExecutionLimits - Execution limits for a tenant
 */
class ExecutionLimits {
    private int maxConcurrentRuns;
    private int maxRunDurationMinutes;
    private int maxWorkflowNodes;
    private long maxMemoryPerRunMB;
    private int maxRetries;
    private int maxNodesPerWorkflow;
    private long maxExecutionTimeMs;

    public ExecutionLimits() {
    }

    public ExecutionLimits(int maxConcurrentRuns, int maxRunDurationMinutes, int maxWorkflowNodes,
            long maxMemoryPerRunMB, int maxRetries) {
        this.maxConcurrentRuns = maxConcurrentRuns;
        this.maxRunDurationMinutes = maxRunDurationMinutes;
        this.maxWorkflowNodes = maxWorkflowNodes;
        this.maxMemoryPerRunMB = maxMemoryPerRunMB;
        this.maxRetries = maxRetries;
    }

    // Getters and setters
    public int getMaxConcurrentRuns() {
        return maxConcurrentRuns;
    }

    public void setMaxConcurrentRuns(int maxConcurrentRuns) {
        this.maxConcurrentRuns = maxConcurrentRuns;
    }

    public int getMaxRunDurationMinutes() {
        return maxRunDurationMinutes;
    }

    public void setMaxRunDurationMinutes(int maxRunDurationMinutes) {
        this.maxRunDurationMinutes = maxRunDurationMinutes;
    }

    public int getMaxWorkflowNodes() {
        return maxWorkflowNodes;
    }

    public void setMaxWorkflowNodes(int maxWorkflowNodes) {
        this.maxWorkflowNodes = maxWorkflowNodes;
    }

    public long getMaxMemoryPerRunMB() {
        return maxMemoryPerRunMB;
    }

    public void setMaxMemoryPerRunMB(long maxMemoryPerRunMB) {
        this.maxMemoryPerRunMB = maxMemoryPerRunMB;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxNodesPerWorkflow() {
        return maxNodesPerWorkflow;
    }

    public void setMaxNodesPerWorkflow(int maxNodesPerWorkflow) {
        this.maxNodesPerWorkflow = maxNodesPerWorkflow;
    }

    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

    public void setMaxExecutionTimeMs(long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }
}

/**
 * TenantNodePermissions - Node permissions for a tenant
 */
class TenantNodePermissions {
    private String tenantId;
    private java.util.Set<String> allowedNodeTypes;
    private java.util.Set<String> blockedNodeTypes;

    public TenantNodePermissions() {
    }

    public TenantNodePermissions(String tenantId, java.util.Set<String> allowedNodeTypes,
            java.util.Set<String> blockedNodeTypes) {
        this.tenantId = tenantId;
        this.allowedNodeTypes = allowedNodeTypes;
        this.blockedNodeTypes = blockedNodeTypes;
    }

    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public java.util.Set<String> getAllowedNodeTypes() {
        return allowedNodeTypes;
    }

    public void setAllowedNodeTypes(java.util.Set<String> allowedNodeTypes) {
        this.allowedNodeTypes = allowedNodeTypes;
    }

    public java.util.Set<String> getBlockedNodeTypes() {
        return blockedNodeTypes;
    }

    public void setBlockedNodeTypes(java.util.Set<String> blockedNodeTypes) {
        this.blockedNodeTypes = blockedNodeTypes;
    }
}

/**
 * TimeoutConfiguration - Timeout configuration for a tenant
 */
class TimeoutConfiguration {
    private long workflowTimeoutMs;
    private long nodeTimeoutMs;
    private long humanTaskTimeoutMs;

    public TimeoutConfiguration() {
    }

    public TimeoutConfiguration(long workflowTimeoutMs, long nodeTimeoutMs, long humanTaskTimeoutMs) {
        this.workflowTimeoutMs = workflowTimeoutMs;
        this.nodeTimeoutMs = nodeTimeoutMs;
        this.humanTaskTimeoutMs = humanTaskTimeoutMs;
    }

    public long getWorkflowTimeoutMs() {
        return workflowTimeoutMs;
    }

    public void setWorkflowTimeoutMs(long workflowTimeoutMs) {
        this.workflowTimeoutMs = workflowTimeoutMs;
    }

    public long getNodeTimeoutMs() {
        return nodeTimeoutMs;
    }

    public void setNodeTimeoutMs(long nodeTimeoutMs) {
        this.nodeTimeoutMs = nodeTimeoutMs;
    }

    public long getHumanTaskTimeoutMs() {
        return humanTaskTimeoutMs;
    }

    public void setHumanTaskTimeoutMs(long humanTaskTimeoutMs) {
        this.humanTaskTimeoutMs = humanTaskTimeoutMs;
    }
}