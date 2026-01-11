package tech.kayys.wayang.mcp.runtime;

import java.util.Map;

/**
 * Tool execution request
 */
public record ToolExecutionRequest(
                String tenantId,
                String userId,
                String toolId,
                Map<String, Object> arguments,
                String workflowRunId,
                String agentId,
                Map<String, Object> context) {
}
