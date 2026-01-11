package tech.kayys.wayang.mcp.dto;

import java.util.Map;

public record OpenApiToolRequest(
                String namespace,
                SourceType sourceType,
                String source,
                String authProfileId,
                Map<String, Object> guardrailsConfig) {
}