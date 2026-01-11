package tech.kayys.wayang.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.mcp.resource.McpGeneratorResource;
import io.quarkus.logging.Log;

@ApplicationScoped
public class SpecificationDetector {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpGeneratorResource.SpecificationType detectSpecificationType(String content, String filename) {
        try {
            JsonNode root = objectMapper.readTree(content);

            // Check for Postman collection indicators
            if (root.has("info") && root.has("item")) {
                JsonNode info = root.get("info");
                if (info.has("name") || info.has("_postman_id") || info.has("schema")) {
                    Log.debug("Detected Postman collection (info + item structure)");
                    return McpGeneratorResource.SpecificationType.POSTMAN;
                }
            }

            // Check for Postman v2.1.0 schema
            if (root.has("info") && root.get("info").has("schema")) {
                String schema = root.get("info").get("schema").asText();
                if (schema.contains("postman")) {
                    Log.debug("Detected Postman collection (schema field contains 'postman')");
                    return McpGeneratorResource.SpecificationType.POSTMAN;
                }
            }

            // Check filename patterns
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.contains("postman") || lowerFilename.endsWith(".postman_collection.json")) {
                    Log.debug("Detected Postman collection based on filename pattern");
                    return McpGeneratorResource.SpecificationType.POSTMAN;
                }

                if (lowerFilename.contains("openapi") || lowerFilename.contains("swagger") ||
                        lowerFilename.endsWith(".openapi.json") || lowerFilename.endsWith(".swagger.json")) {
                    Log.debug("Detected OpenAPI specification based on filename pattern");
                    return McpGeneratorResource.SpecificationType.OPENAPI;
                }
            }

            // Default fallback - try OpenAPI first
            if (root.has("paths") || root.has("components")) {
                Log.debug("Fallback detection: OpenAPI (has paths or components)");
                return McpGeneratorResource.SpecificationType.OPENAPI;
            }

            Log.warn("Unable to detect specification type, defaulting to OpenAPI");
            return McpGeneratorResource.SpecificationType.OPENAPI;

        } catch (Exception e) {
            Log.error("Error detecting specification type", e);
            return McpGeneratorResource.SpecificationType.OPENAPI;
        }
    }
}
