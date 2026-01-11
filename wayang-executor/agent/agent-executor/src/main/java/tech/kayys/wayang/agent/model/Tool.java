package tech.kayys.wayang.agent.model;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * ============================================================================
 * AGENT TOOLS SYSTEM
 * ============================================================================
 * 
 * Comprehensive tool/function calling system for agents.
 * 
 * Features:
 * - Dynamic tool registration and discovery
 * - Multi-tenant tool isolation
 * - Parameter validation
 * - Async execution
 * - Error handling and retries
 * - Tool metrics and monitoring
 * 
 * Built-in Tools:
 * - Calculator: Mathematical operations
 * - WebSearch: Search the web
 * - DatabaseQuery: Query databases
 * - APICall: Call external APIs
 * - FileOperation: Read/write files
 * - CodeExecutor: Execute code snippets
 * 
 * Architecture:
 * ┌─────────────────────────────────────────────────────────┐
 * │                  Tool Registry                          │
 * ├─────────────────────────────────────────────────────────┤
 * │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐ │
 * │  │Calculator│  │WebSearch │  │Database  │  │ Custom │ │
 * │  │   Tool   │  │   Tool   │  │  Query   │  │  Tool  │ │
 * │  └──────────┘  └──────────┘  └──────────┘  └────────┘ │
 * │       │             │              │            │       │
 * │  ┌────▼─────────────▼──────────────▼────────────▼───┐ │
 * │  │         Tool Execution Engine                    │ │
 * │  └──────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────┘
 */

// ==================== TOOL INTERFACE ====================

/**
 * Base interface for all agent tools
 */
public interface Tool {

    /**
     * Get tool name (must be unique per tenant)
     */
    String name();

    /**
     * Get tool description for LLM
     */
    String description();

    /**
     * Get parameter schema (JSON Schema format)
     */
    Map<String, Object> parameterSchema();

    /**
     * Convert to LLM tool definition
     */
    default ToolDefinition toToolDefinition() {
        return ToolDefinition.create(name(), description(), parameterSchema());
    }

    /**
     * Validate tool arguments
     */
    Uni<Boolean> validate(Map<String, Object> arguments);

    /**
     * Execute the tool
     */
    Uni<String> execute(Map<String, Object> arguments, AgentContext context);

    /**
     * Get tool metadata
     */
    default Map<String, Object> metadata() {
        return Map.of(
                "name", name(),
                "description", description(),
                "version", "1.0.0");
    }

    /**
     * Check if tool requires authentication
     */
    default boolean requiresAuth() {
        return false;
    }

    /**
     * Check if tool is async (long-running)
     */
    default boolean isAsync() {
        return false;
    }
}
