package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record OpenAPISpec(
                String version,
                Map<String, Object> paths
// ... parsed spec
) {
}