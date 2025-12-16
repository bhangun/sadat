package tech.kayys.wayang.plugin.node;


import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.ConfigurationSchema;
import tech.kayys.wayang.plugin.DependencyCheckResult;
import tech.kayys.wayang.plugin.HealthCheckResult;
import tech.kayys.wayang.plugin.PerformanceProfile;
import tech.kayys.wayang.plugin.Plugin;
import tech.kayys.wayang.plugin.PluginContext;
import tech.kayys.wayang.plugin.PluginDependency;
import tech.kayys.wayang.plugin.PluginDocumentation;
import tech.kayys.wayang.plugin.PluginEvent;
import tech.kayys.wayang.plugin.PluginEventListener;
import tech.kayys.wayang.plugin.PluginInfo;
import tech.kayys.wayang.plugin.PluginMetrics;
import tech.kayys.wayang.plugin.PluginState;
import tech.kayys.wayang.plugin.ReadinessCheckResult;
import tech.kayys.wayang.plugin.RecoveryResult;
import tech.kayys.wayang.plugin.error.ErrorHandlingDecision;
import tech.kayys.wayang.plugin.resource.ResourceRequirements;
import tech.kayys.wayang.plugin.resource.ResourceUsage;
import tech.kayys.wayang.plugin.validation.ValidationResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * Enhanced Node Plugin Interface - Provides custom node types with advanced capabilities
 * 
 * Improvements over basic version:
 * - Lifecycle management (init, start, stop, destroy)
 * - Health checks and readiness
 * - Metrics and monitoring hooks
 * - Resource management and cleanup
 * - Dependency injection support
 * - Hot-reload support
 * - Configuration validation
 * - Error handling hooks
 * - Event emission
 * - Multi-tenancy support
 * 
 * @version 2.0.0
 * @since 1.0.0
 */
public interface NodePlugin extends Plugin {

    // ========================================================================
    // Core Node Registration (Enhanced)
    // ========================================================================

    /**
     * Get provided node factories with metadata
     * 
     * @return Map of node type ID to factory with registration metadata
     */
    Map<String, NodeFactoryRegistration> getNodeFactories();

    /**
     * Get node descriptors with full schema and capabilities
     * 
     * @return List of complete node descriptors with validation rules
     */
    
    List<NodeDescriptor> getNodeDescriptors();

    /**
     * Get node categories for organization in UI
     * 
     * @return Map of category ID to category metadata
     */
    default Map<String, NodeCategory> getNodeCategories() {
        return Map.of(
            "default", NodeCategory.builder()
                .id("default")
                .name("General")
                .description("General purpose nodes")
                .icon("folder")
                .order(0)
                .build()
        );
    }

    // ========================================================================
    // Lifecycle Management
    // ========================================================================

    /**
     * Initialize plugin - called once after plugin is loaded
     * 
     * @param context Plugin initialization context with configuration
     * @return Uni that completes when initialization is done
     */
    default Uni<Void> initialize( PluginContext context) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Start plugin - called when plugin becomes active
     * 
     * @return Uni that completes when plugin is ready to accept requests
     */
    default Uni<Void> start() {
        return Uni.createFrom().voidItem();
    }

    /**
     * Stop plugin - called before plugin is unloaded
     * 
     * @param timeout Maximum time to wait for graceful shutdown
     * @return Uni that completes when plugin has stopped
     */
    default Uni<Void> stop(Duration timeout) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Destroy plugin - final cleanup before unload
     * 
     * @return Uni that completes when cleanup is done
     */
    default Uni<Void> destroy() {
        return Uni.createFrom().voidItem();
    }

    // ========================================================================
    // Health & Readiness
    // ========================================================================

    /**
     * Check if plugin is healthy
     * 
     * @return Health check result with details
     */
    default Uni<HealthCheckResult> healthCheck() {
        return Uni.createFrom().item(
            HealthCheckResult.healthy("Plugin is running")
        );
    }

    /**
     * Check if plugin is ready to accept requests
     * 
     * @return Readiness check result
     */
    default Uni<ReadinessCheckResult> readinessCheck() {
        return Uni.createFrom().item(
            ReadinessCheckResult.ready("Plugin is ready")
        );
    }

    /**
     * Get plugin metrics for monitoring
     * 
     * @return Current plugin metrics
     */
    default PluginMetrics getMetrics() {
        return PluginMetrics.builder()
            .requestCount(0)
            .errorCount(0)
            .averageLatencyMs(0.0)
            .build();
    }

    // ========================================================================
    // Configuration & Validation
    // ========================================================================

    /**
     * Get plugin configuration schema
     * 
     * @return JSON schema for plugin configuration
     */
    default Optional<ConfigurationSchema> getConfigurationSchema() {
        return Optional.empty();
    }

    /**
     * Validate plugin configuration
     * 
     * @param config Configuration to validate
     * @return Validation result with errors if any
     */
    default ValidationResult validateConfiguration( Map<String, Object> config) {
        return ValidationResult.valid();
    }

    /**
     * Get current plugin configuration
     * 
     * @return Current configuration map
     */
    default Map<String, Object> getConfiguration() {
        return Map.of();
    }

    /**
     * Update plugin configuration at runtime
     * 
     * @param config New configuration
     * @return Uni that completes when configuration is applied
     */
    default Uni<Void> updateConfiguration( Map<String, Object> config) {
        return Uni.createFrom().voidItem();
    }

    // ========================================================================
    // Resource Management
    // ========================================================================

    /**
     * Get resource requirements for this plugin
     * 
     * @return Resource requirements (CPU, memory, etc.)
     */
    default ResourceRequirements getResourceRequirements() {
        return ResourceRequirements.builder()
            .cpu("100m")
            .memory("128Mi")
            .ephemeralStorage("100Mi")
            .build();
    }

    /**
     * Cleanup resources for specific tenant
     * 
     * @param tenantId Tenant ID to cleanup
     * @return Uni that completes when cleanup is done
     */
    default Uni<Void> cleanupTenantResources( String tenantId) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Get current resource usage
     * 
     * @return Current resource usage metrics
     */
    default ResourceUsage getCurrentResourceUsage() {
        return ResourceUsage.builder()
            .cpuUsagePercent(0.0)
            .memoryUsageMb(0.0)
            .build();
    }

    // ========================================================================
    // Dependency Management
    // ========================================================================

    /**
     * Get plugin dependencies
     * 
     * @return List of required plugin dependencies
     */
    default List<PluginDependency> getDependencies() {
        return List.of();
    }

    /**
     * Check if dependencies are satisfied
     * 
     * @param availablePlugins Currently available plugins
     * @return Dependency check result
     */
    default DependencyCheckResult checkDependencies(
             Set<PluginInfo> availablePlugins) {
        
        List<PluginDependency> dependencies = getDependencies();
        List<String> missing = new ArrayList<>();
        
        for (PluginDependency dep : dependencies) {
            boolean found = availablePlugins.stream()
                .anyMatch(p -> p.getId().equals(dep.getPluginId()) &&
                             dep.versionMatches(p.getVersion()));
            
            if (!found && dep.isRequired()) {
                missing.add(dep.getPluginId() + ":" + dep.getVersionRange());
            }
        }
        
        return missing.isEmpty() 
            ? DependencyCheckResult.satisfied()
            : DependencyCheckResult.unsatisfied(missing);
    }

    // ========================================================================
    // Error Handling & Recovery
    // ========================================================================

    /**
     * Handle node execution error (hook for custom error handling)
     * 
     * @param nodeId Node that failed
     * @param error Error that occurred
     * @param context Execution context
     * @return Error handling decision
     */
    default Uni<ErrorHandlingDecision> handleNodeError(
             String nodeId,
             Throwable error,
             NodeContext context) {
        
        // Default: let platform handle it
        return Uni.createFrom().item(
            ErrorHandlingDecision.delegate()
        );
    }

    /**
     * Attempt to recover from error
     * 
     * @param nodeId Node that failed
     * @param error Error that occurred
     * @return Recovery result
     */
    default Uni<RecoveryResult> attemptRecovery(
             String nodeId,
             Throwable error) {
        
        return Uni.createFrom().item(
            RecoveryResult.failed("No recovery strategy available")
        );
    }

    // ========================================================================
    // Event Emission
    // ========================================================================

    /**
     * Register event listener for plugin events
     * 
     * @param listener Event listener
     */
    default void addEventListener( PluginEventListener listener) {
        // Default: no-op
    }

    /**
     * Remove event listener
     * 
     * @param listener Event listener to remove
     */
    default void removeEventListener( PluginEventListener listener) {
        // Default: no-op
    }

    /**
     * Emit custom plugin event
     * 
     * @param event Event to emit
     */
    default void emitEvent( PluginEvent event) {
        // Default: no-op
    }

    // ========================================================================
    // Hot Reload Support
    // ========================================================================

    /**
     * Check if plugin supports hot reload
     * 
     * @return true if hot reload is supported
     */
    default boolean supportsHotReload() {
        return false;
    }

    /**
     * Prepare for hot reload
     * 
     * @return State to transfer to new version
     */
    default Uni<PluginState> prepareForReload() {
        return Uni.createFrom().item(PluginState.empty());
    }

    /**
     * Restore state after hot reload
     * 
     * @param state State from previous version
     * @return Uni that completes when state is restored
     */
    default Uni<Void> restoreState( PluginState state) {
        return Uni.createFrom().voidItem();
    }

    // ========================================================================
    // Multi-Tenancy Support
    // ========================================================================

    /**
     * Check if plugin supports multi-tenancy
     * 
     * @return true if multi-tenancy is supported
     */
    default boolean isMultiTenantAware() {
        return false;
    }

    /**
     * Initialize tenant-specific resources
     * 
     * @param tenantId Tenant ID
     * @param tenantConfig Tenant-specific configuration
     * @return Uni that completes when tenant is initialized
     */
    default Uni<Void> initializeTenant(
             String tenantId,
             Map<String, Object> tenantConfig) {
        
        return Uni.createFrom().voidItem();
    }

    /**
     * Get tenant-specific node factory
     * 
     * @param nodeType Node type ID
     * @param tenantId Tenant ID
     * @return Tenant-specific node factory
     */
    default Optional<NodeFactory> getTenantNodeFactory(
             String nodeType,
             String tenantId) {
        
        return getNodeFactories().containsKey(nodeType)
            ? Optional.of(getNodeFactories().get(nodeType).getFactory())
            : Optional.empty();
    }

    // ========================================================================
    // Advanced Capabilities
    // ========================================================================

    /**
     * Get plugin capabilities (features this plugin supports)
     * 
     * @return Set of capability identifiers
     */
    default Set<String> getCapabilities() {
        return Set.of(
            "basic-execution",  // Can execute nodes
            "error-handling"    // Basic error handling
        );
    }

    /**
     * Check if plugin has specific capability
     * 
     * @param capability Capability to check
     * @return true if capability is supported
     */
    default boolean hasCapability( String capability) {
        return getCapabilities().contains(capability);
    }

    /**
     * Get plugin performance profile
     * 
     * @return Performance characteristics
     */
    default PerformanceProfile getPerformanceProfile() {
        return PerformanceProfile.builder()
            .averageLatencyMs(10.0)
            .throughputRps(100.0)
            .p95LatencyMs(50.0)
            .p99LatencyMs(100.0)
            .build();
    }

    /**
     * Warm up plugin (preload resources, caches, etc.)
     * 
     * @return Uni that completes when warmup is done
     */
    default Uni<Void> warmUp() {
        return Uni.createFrom().voidItem();
    }

    /**
     * Get plugin documentation
     * 
     * @return Documentation metadata
     */
    default PluginDocumentation getDocumentation() {
        return PluginDocumentation.builder()
            .description("No description available")
            .examples(List.of())
            .build();
    }
}
