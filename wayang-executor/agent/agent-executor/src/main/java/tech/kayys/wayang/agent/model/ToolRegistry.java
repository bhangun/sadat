package tech.kayys.wayang.agent.model;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * Central registry for managing tools
 */
public interface ToolRegistry {

    /**
     * Register a tool
     */
    void registerTool(Tool tool, String tenantId);

    /**
     * Get a specific tool
     */
    Uni<Tool> getTool(String name, String tenantId);

    /**
     * Get multiple tools
     */
    Uni<List<Tool>> getTools(List<String> names, String tenantId);

    /**
     * Get all tools for tenant
     */
    Uni<List<Tool>> getAllTools(String tenantId);

    /**
     * Unregister a tool
     */
    void unregisterTool(String name, String tenantId);

    /**
     * Check if tool exists
     */
    boolean hasTool(String name, String tenantId);
}
