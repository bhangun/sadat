package tech.kayys.wayang.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.mcp.model.ApiOperation;
import tech.kayys.wayang.mcp.model.ApiParameter;
import tech.kayys.wayang.mcp.model.ApiSpecification;

import java.net.URI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class PostmanParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public ApiSpecification parsePostmanCollection(String content, String collectionName) {
        try {
            JsonNode root = objectMapper.readTree(content);

            ApiSpecification apiSpec = new ApiSpecification();

            // Parse collection info
            parseCollectionInfo(root, apiSpec, collectionName);

            // Parse variables for base URL detection
            Map<String, String> variables = parseVariables(root);

            // Parse items (requests)
            List<ApiOperation> operations = new ArrayList<>();
            if (root.has("item")) {
                parseItems(root.get("item"), operations, variables, "");
            }

            apiSpec.setOperations(operations);

            // Extract base URL from first operation if not set
            if (apiSpec.getBaseUrl() == null && !operations.isEmpty()) {
                String baseUrl = extractBaseUrlFromOperations(operations);
                apiSpec.setBaseUrl(baseUrl);
            }

            Log.info("Parsed Postman collection: " + operations.size() + " operations");
            return apiSpec;

        } catch (Exception e) {
            Log.error("Failed to parse Postman collection", e);
            throw new RuntimeException("Failed to parse Postman collection: " + e.getMessage(), e);
        }
    }

    private void parseCollectionInfo(JsonNode root, ApiSpecification apiSpec, String fallbackName) {
        if (root.has("info")) {
            JsonNode info = root.get("info");

            String name = info.has("name") ? info.get("name").asText() : fallbackName;
            apiSpec.setTitle(name);

            if (info.has("description")) {
                JsonNode desc = info.get("description");
                String description = desc.isTextual() ? desc.asText()
                        : desc.has("content") ? desc.get("content").asText() : "";
                apiSpec.setDescription(description);
            }

            if (info.has("version")) {
                apiSpec.setVersion(info.get("version").asText());
            } else {
                apiSpec.setVersion("1.0.0");
            }
        } else {
            apiSpec.setTitle(fallbackName);
            apiSpec.setDescription("Imported from Postman collection");
            apiSpec.setVersion("1.0.0");
        }
    }

    private Map<String, String> parseVariables(JsonNode root) {
        Map<String, String> variables = new HashMap<>();

        // Parse collection-level variables
        if (root.has("variable")) {
            JsonNode variablesNode = root.get("variable");
            if (variablesNode.isArray()) {
                for (JsonNode var : variablesNode) {
                    if (var.has("key") && var.has("value")) {
                        variables.put(var.get("key").asText(), var.get("value").asText());
                    }
                }
            }
        }

        return variables;
    }

    private void parseItems(JsonNode items, List<ApiOperation> operations, Map<String, String> variables,
            String pathPrefix) {
        if (!items.isArray())
            return;

        for (JsonNode item : items) {
            if (item.has("item")) {
                // This is a folder, recurse
                String folderName = item.has("name") ? item.get("name").asText() : "";
                String newPrefix = pathPrefix.isEmpty() ? folderName : pathPrefix + "/" + folderName;
                parseItems(item.get("item"), operations, variables, newPrefix);
            } else if (item.has("request")) {
                // This is a request
                ApiOperation operation = parseRequest(item, variables, pathPrefix);
                if (operation != null) {
                    operations.add(operation);
                }
            }
        }
    }

    private ApiOperation parseRequest(JsonNode item, Map<String, String> variables, String pathPrefix) {
        JsonNode request = item.get("request");
        if (!request.has("url") || !request.has("method")) {
            Log.warn("Skipping request without URL or method: "
                    + (item.has("name") ? item.get("name").asText() : "unnamed"));
            return null;
        }

        ApiOperation operation = new ApiOperation();

        // Set basic info
        String name = item.has("name") ? item.get("name").asText() : "unnamed_request";
        operation.setOperationId(sanitizeOperationId(name));
        operation.setSummary(name);

        if (item.has("description")) {
            JsonNode desc = item.get("description");
            String description = desc.isTextual() ? desc.asText()
                    : desc.has("content") ? desc.get("content").asText() : "";
            operation.setDescription(description);
        }

        // Parse method
        String method = request.get("method").asText().toUpperCase();
        operation.setMethod(method);

        // Parse URL
        String url = parseUrl(request.get("url"), variables);
        String[] urlParts = parseUrlParts(url);
        operation.setPath(urlParts[1]); // path part

        // Parse parameters
        List<ApiParameter> parameters = new ArrayList<>();

        // URL parameters (path and query)
        parseUrlParameters(request.get("url"), parameters);

        // Headers
        if (request.has("header")) {
            parseHeaders(request.get("header"), parameters);
        }

        // Body
        if (request.has("body") && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
            parseBody(request.get("body"), parameters);
        }

        operation.setParameters(parameters);

        // Set default response types
        Map<String, String> responseTypes = new HashMap<>();
        responseTypes.put("200", "Success");
        responseTypes.put("400", "Bad Request");
        responseTypes.put("500", "Internal Server Error");
        operation.setResponseTypes(responseTypes);

        return operation;
    }

    private String parseUrl(JsonNode urlNode, Map<String, String> variables) {
        if (urlNode.isTextual()) {
            return replaceVariables(urlNode.asText(), variables);
        }

        if (urlNode.has("raw")) {
            return replaceVariables(urlNode.get("raw").asText(), variables);
        }

        // Construct from parts
        StringBuilder url = new StringBuilder();

        if (urlNode.has("protocol")) {
            url.append(urlNode.get("protocol").asText()).append("://");
        } else {
            url.append("https://");
        }

        if (urlNode.has("host")) {
            JsonNode host = urlNode.get("host");
            if (host.isArray()) {
                for (int i = 0; i < host.size(); i++) {
                    if (i > 0)
                        url.append(".");
                    url.append(host.get(i).asText());
                }
            } else {
                url.append(host.asText());
            }
        }

        if (urlNode.has("port")) {
            url.append(":").append(urlNode.get("port").asText());
        }

        if (urlNode.has("path")) {
            JsonNode path = urlNode.get("path");
            if (path.isArray()) {
                for (JsonNode segment : path) {
                    url.append("/").append(segment.asText());
                }
            } else {
                url.append(path.asText());
            }
        }

        if (urlNode.has("query")) {
            url.append("?");
            JsonNode query = urlNode.get("query");
            if (query.isArray()) {
                boolean first = true;
                for (JsonNode param : query) {
                    if (!first)
                        url.append("&");
                    url.append(param.get("key").asText()).append("=").append(param.get("value").asText());
                    first = false;
                }
            }
        }

        return replaceVariables(url.toString(), variables);
    }

    private String replaceVariables(String text, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, "{{" + varName + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String[] parseUrlParts(String fullUrl) {
        try {

            URI url = new URI(fullUrl);

            String baseUrl = url.getScheme() + "://" + url.getHost();
            if (url.getPort() != -1) {
                baseUrl += ":" + url.getPort();
            }
            String path = url.getPath();
            if (path.isEmpty()) {
                path = "/";
            }
            return new String[] { baseUrl, path };
        } catch (Exception e) {
            Log.warn("Failed to parse URL: " + fullUrl + ", using fallback");
            int pathStart = fullUrl.indexOf('/', 8); // Skip protocol://
            if (pathStart != -1) {
                return new String[] { fullUrl.substring(0, pathStart), fullUrl.substring(pathStart) };
            } else {
                return new String[] { fullUrl, "/" };
            }
        }
    }

    private void parseUrlParameters(JsonNode urlNode, List<ApiParameter> parameters) {
        // Parse path variables
        if (urlNode.has("variable")) {
            JsonNode variables = urlNode.get("variable");
            if (variables.isArray()) {
                for (JsonNode var : variables) {
                    if (var.has("key")) {
                        ApiParameter param = new ApiParameter();
                        param.setName(var.get("key").asText());
                        param.setIn("path");
                        param.setRequired(true);
                        param.setType("string");
                        param.setDescription(
                                var.has("description") ? var.get("description").asText() : "Path parameter");
                        parameters.add(param);
                    }
                }
            }
        }

        // Parse query parameters
        if (urlNode.has("query")) {
            JsonNode query = urlNode.get("query");
            if (query.isArray()) {
                for (JsonNode param : query) {
                    if (param.has("key")) {
                        ApiParameter apiParam = new ApiParameter();
                        apiParam.setName(param.get("key").asText());
                        apiParam.setIn("query");
                        apiParam.setRequired(false);
                        apiParam.setType("string");
                        apiParam.setDescription(
                                param.has("description") ? param.get("description").asText() : "Query parameter");
                        if (param.has("value")) {
                            apiParam.setExample(param.get("value").asText());
                        }
                        parameters.add(apiParam);
                    }
                }
            }
        }
    }

    private void parseHeaders(JsonNode headers, List<ApiParameter> parameters) {
        if (!headers.isArray())
            return;

        for (JsonNode header : headers) {
            if (header.has("key") && header.has("value")) {
                String key = header.get("key").asText();

                // Skip standard headers that are usually handled by HTTP client
                if (isStandardHeader(key)) {
                    continue;
                }

                ApiParameter param = new ApiParameter();
                param.setName(key);
                param.setIn("header");
                param.setRequired(false);
                param.setType("string");
                param.setDescription("Header parameter");
                param.setExample(header.get("value").asText());
                parameters.add(param);
            }
        }
    }

    private void parseBody(JsonNode body, List<ApiParameter> parameters) {
        if (!body.has("mode"))
            return;

        String mode = body.get("mode").asText();

        switch (mode) {
            case "raw":
                if (body.has("raw")) {
                    ApiParameter param = new ApiParameter();
                    param.setName("body");
                    param.setIn("body");
                    param.setRequired(true);
                    param.setType("object");
                    param.setDescription("Request body");
                    param.setExample(body.get("raw").asText());
                    parameters.add(param);
                }
                break;

            case "formdata":
            case "urlencoded":
                if (body.has(mode)) {
                    JsonNode formData = body.get(mode);
                    if (formData.isArray()) {
                        for (JsonNode field : formData) {
                            if (field.has("key")) {
                                ApiParameter param = new ApiParameter();
                                param.setName(field.get("key").asText());
                                param.setIn("formData");
                                param.setRequired(false);
                                param.setType("string");
                                param.setDescription("Form parameter");
                                if (field.has("value")) {
                                    param.setExample(field.get("value").asText());
                                }
                                parameters.add(param);
                            }
                        }
                    }
                }
                break;
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

    private String sanitizeOperationId(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String extractBaseUrlFromOperations(List<ApiOperation> operations) {
        // This would need access to the full URLs from operations
        // For now, return a default
        return "http://localhost:8080";
    }
}
