package tech.kayys.wayang.mcp.dto;

import java.util.Map;

public record ToolExecutionResponse(
        String status,
        Map<String, Object> output,
        String error,
        long executionTimeMs) {
}