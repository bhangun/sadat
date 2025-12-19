package tech.kayys.wayang.workflow.service;

/**
 * Registry statistics
 */
@lombok.Data
@lombok.Builder
class RegistryStatistics {
    private int totalNodes;
    private long builtInNodes;
    private long pluginNodes;
    private int cachedNodes;
}