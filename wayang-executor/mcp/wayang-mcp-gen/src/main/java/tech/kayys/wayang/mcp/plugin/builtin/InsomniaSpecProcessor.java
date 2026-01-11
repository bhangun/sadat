package tech.kayys.wayang.mcp.plugin.builtin;

import tech.kayys.wayang.mcp.model.ApiSpecification;
import tech.kayys.wayang.mcp.model.ApiOperation;
import tech.kayys.wayang.mcp.model.ApiParameter;
import tech.kayys.wayang.mcp.plugin.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.*;

@ApplicationScoped
public class InsomniaSpecProcessor implements SpecificationProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSpecificationType() {
        return "insomnia";
    }

    @Override
    public void initialize() throws PluginException {
        // Initialization logic
    }

    @Override
    public boolean canProcess(String content, String filename) {
        try {
            JsonNode root = objectMapper.readTree(content);

            // Check for Insomnia-specific fields
            if (root.has("_type") && root.get("_type").asText().equals("export")) {
                return true;
            }

            if (root.has("resources") && root.has("__export_format")) {
                return true;
            }

            // Check filename
            if (filename != null && filename.toLowerCase().contains("insomnia")) {
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ApiSpecification processSpecification(InputStream content, String filename,
            PluginExecutionContext context) throws PluginException {
        try {
            String contentStr = new String(content.readAllBytes());
            JsonNode root = objectMapper.readTree(contentStr);

            context.log("INFO", "Processing Insomnia export file: %s", filename);

            ApiSpecification apiSpec = new ApiSpecification();

            // Extract workspace info
            JsonNode workspace = findWorkspace(root);
            if (workspace != null) {
                apiSpec.setTitle(workspace.has("name") ? workspace.get("name").asText() : "Insomnia API");
                apiSpec.setDescription(workspace.has("description") ? workspace.get("description").asText() : "");
            } else {
                apiSpec.setTitle("Insomnia API");
                apiSpec.setDescription("Imported from Insomnia");
            }
            apiSpec.setVersion("1.0.0");

            // Extract base URL from environment
            String baseUrl = extractBaseUrl(root);
            apiSpec.setBaseUrl(baseUrl != null ? baseUrl : "http://localhost:8080");

            // Process requests
            List<ApiOperation> operations = processRequests(root, context);
            apiSpec.setOperations(operations);

            context.log("INFO", "Processed %d operations from Insomnia export", operations.size());

            return apiSpec;

        } catch (Exception e) {
            throw new PluginException("insomnia-processor", "process",
                    "Failed to process Insomnia specification", e);
        }
    }

    @Override
    public ValidationResult validateSpecification(InputStream content, String filename,
            PluginExecutionContext context) throws PluginException {
        try {
            String contentStr = new String(content.readAllBytes());
            JsonNode root = objectMapper.readTree(contentStr);

            ValidationResult result = new ValidationResult(true);

            if (!canProcess(contentStr, filename)) {
                return result.addError("Not a valid Insomnia export file");
            }

            // Check for resources
            if (!root.has("resources")) {
                result.addWarning("No resources found in Insomnia export");
            } else {
                JsonNode resources = root.get("resources");
                if (!resources.isArray() || resources.size() == 0) {
                    result.addWarning("Resources array is empty");
                }
            }

            return result;

        } catch (Exception e) {
            throw new PluginException("insomnia-processor", "validate",
                    "Failed to validate Insomnia specification", e);
        }
    }

    private JsonNode findWorkspace(JsonNode root) {
        if (root.has("resources") && root.get("resources").isArray()) {
            for (JsonNode resource : root.get("resources")) {
                if (resource.has("_type") && resource.get("_type").asText().equals("workspace")) {
                    return resource;
                }
            }
        }
        return null;
    }

    private String extractBaseUrl(JsonNode root) {
        if (root.has("resources") && root.get("resources").isArray()) {
            for (JsonNode resource : root.get("resources")) {
                if (resource.has("_type") && resource.get("_type").asText().equals("environment")) {
                    if (resource.has("data") && resource.get("data").has("base_url")) {
                        return resource.get("data").get("base_url").asText();
                    }
                }
            }
        }
        return null;
    }

    private List<ApiOperation> processRequests(JsonNode root, PluginExecutionContext context) {
        List<ApiOperation> operations = new ArrayList<>();

        if (root.has("resources") && root.get("resources").isArray()) {
            for (JsonNode resource : root.get("resources")) {
                if (resource.has("_type") && resource.get("_type").asText().equals("request")) {
                    try {
                        ApiOperation operation = processRequest(resource);
                        if (operation != null) {
                            operations.add(operation);
                        }
                    } catch (Exception e) {
                        context.log("WARN", "Failed to process request: %s", e.getMessage());
                    }
                }
            }
        }

        return operations;
    }

    private ApiOperation processRequest(JsonNode request) {
        ApiOperation operation = new ApiOperation();

        // Basic info
        String name = request.has("name") ? request.get("name").asText() : "unnamed_request";
        operation.setOperationId(sanitizeOperationId(name));
        operation.setSummary(name);
        operation.setDescription(request.has("description") ? request.get("description").asText() : "");

        // Method
        String method = request.has("method") ? request.get("method").asText() : "GET";
        operation.setMethod(method.toUpperCase());

        // URL and path
        String url = request.has("url") ? request.get("url").asText() : "/";
        operation.setPath(extractPath(url));

        // Parameters
        List<ApiParameter> parameters = new ArrayList<>();

        // URL parameters
        if (request.has("parameters")) {
            processUrlParameters(request.get("parameters"), parameters);
        }

        // Headers
        if (request.has("headers")) {
            processHeaders(request.get("headers"), parameters);
        }

        // Body
        if (request.has("body")) {
            processBody(request.get("body"), parameters);
        }

        operation.setParameters(parameters);

        // Default responses
        Map<String, String> responseTypes = new HashMap<>();
        responseTypes.put("200", "Success");
        responseTypes.put("400", "Bad Request");
        responseTypes.put("500", "Internal Server Error");
        operation.setResponseTypes(responseTypes);

        return operation;
    }

    private String sanitizeOperationId(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String extractPath(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String path = parsedUrl.getPath();
            return path.isEmpty() ? "/" : path;
        } catch (Exception e) {
            // If URL parsing fails, try to extract path manually
            int pathStart = url.indexOf('/', 8); // Skip protocol://
            if (pathStart != -1) {
                int queryStart = url.indexOf('?', pathStart);
                return queryStart != -1 ? url.substring(pathStart, queryStart) : url.substring(pathStart);
            }
            return "/";
        }
    }

    private void processUrlParameters(JsonNode parameters, List<ApiParameter> paramList) {
        if (parameters.isArray()) {
            for (JsonNode param : parameters) {
                if (param.has("name") && param.has("value")) {
                    ApiParameter apiParam = new ApiParameter();
                    apiParam.setName(param.get("name").asText());
                    apiParam.setIn("query");
                    apiParam.setRequired(false);
                    apiParam.setType("string");
                    apiParam.setDescription("Query parameter");
                    apiParam.setExample(param.get("value").asText());
                    paramList.add(apiParam);
                }
            }
        }
    }

    private void processHeaders(JsonNode headers, List<ApiParameter> paramList) {
        if (headers.isArray()) {
            for (JsonNode header : headers) {
                if (header.has("name") && header.has("value")) {
                    String name = header.get("name").asText();

                    // Skip common headers
                    if (isStandardHeader(name)) {
                        continue;
                    }

                    ApiParameter apiParam = new ApiParameter();
                    apiParam.setName(name);
                    apiParam.setIn("header");
                    apiParam.setRequired(false);
                    apiParam.setType("string");
                    apiParam.setDescription("Header parameter");
                    apiParam.setExample(header.get("value").asText());
                    paramList.add(apiParam);
                }
            }
        }
    }

    private void processBody(JsonNode body, List<ApiParameter> paramList) {
        if (body.has("mimeType")) {
            String mimeType = body.get("mimeType").asText();

            if ("application/json".equals(mimeType) && body.has("text")) {
                ApiParameter bodyParam = new ApiParameter();
                bodyParam.setName("requestBody");
                bodyParam.setIn("body");
                bodyParam.setRequired(true);
                bodyParam.setType("object");
                bodyParam.setDescription("Request body");
                bodyParam.setExample(body.get("text").asText());
                paramList.add(bodyParam);
            }
        }
    }

    private boolean isStandardHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.equals("content-type") ||
                lower.equals("content-length") ||
                lower.equals("host") ||
                lower.equals("user-agent") ||
                lower.equals("accept") ||
                lower.equals("accept-encoding") ||
                lower.equals("connection");
    }
}
