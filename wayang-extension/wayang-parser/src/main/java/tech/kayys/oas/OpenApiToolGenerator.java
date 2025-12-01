

// tech.kayys.platform.tool.OpenApiToolGenerator.java
package tech.kayys.platform.tool;

import tech.kayys.platform.schema.*;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.*;

public class OpenApiToolGenerator {
    private final ObjectMapper mapper;

    public OpenApiToolGenerator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<ToolDefinition> generateTools(OpenApiSpec spec) {
        List<ToolDefinition> tools = new ArrayList<>();
        OpenAPI openApi = spec.openApi();

        for (String path : openApi.getPaths().keySet()) {
            var pathItem = openApi.getPaths().get(path);
            for (var method : pathItem.readOperationsMap().entrySet()) {
                String httpMethod = method.getKey().name();
                Operation operation = method.getValue();

                // Build tool ID: {spec-id}.{operationId}
                String operationId = operation.getOperationId() != null
                    ? operation.getOperationId()
                    : sanitize(path + "_" + httpMethod);

                String toolId = "openapi." + spec.sourceUrl().getHost() + "." + operationId;

                // Build parameters from OpenAPI spec
                List<ActionParameter> params = buildParameters(operation, path, httpMethod);

                // Security: require "tool:openapi:{toolId}:execute"
                SecurityPolicy security = new SecurityPolicy(
                    List.of("tool:openapi:" + toolId + ":execute"),
                    "restricted",
                    List.of()
                );

                ToolDefinition tool = ToolDefinition.builder()
                    .id(toolId)
                    .name(operation.getSummary() != null ? operation.getSummary() : operationId)
                    .description(Optional.ofNullable(operation.getDescription()))
                    .parameters(params)
                    .produces(List.of("application/json")) // OpenAPI typically returns JSON
                    .securityPolicy(security)
                    .build();

                tools.add(tool);
            }
        }
        return tools;
    }

    private List<ActionParameter> buildParameters(Operation operation, String path, String httpMethod) {
        List<ActionParameter> params = new ArrayList<>();

        // Path parameters (e.g., /users/{id})
        for (String segment : path.split("/")) {
            if (segment.startsWith("{") && segment.endsWith("}")) {
                String paramName = segment.substring(1, segment.length() - 1);
                params.add(new ActionParameter(
                    paramName,
                    "string", // OpenAPI path params are always strings
                    true,
                    Optional.of("Path parameter for " + path),
                    mapper.createObjectNode()
                ));
            }
        }

        // Query, header, cookie parameters
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if ("query".equals(param.getIn()) || "header".equals(param.getIn())) {
                    Schema<?> schema = param.getSchema();
                    String type = schema != null ? mapOpenApiType(schema) : "string";
                    params.add(new ActionParameter(
                        param.getName(),
                        type,
                        param.getRequired() != null ? param.getRequired() : false,
                        Optional.ofNullable(param.getDescription()),
                        mapper.createObjectNode()
                    ));
                }
            }
        }

        // Request body (for POST/PUT)
        if (operation.getRequestBody() != null) {
            // For simplicity, treat as single "body" parameter
            params.add(new ActionParameter(
                "body",
                "object",
                true,
                Optional.of("Request body"),
                mapper.createObjectNode() // could embed full JSON Schema here
            ));
        }

        return params;
    }

    private String mapOpenApiType(Schema<?> schema) {
        if (schema.getType() != null) {
            return switch (schema.getType()) {
                case "integer", "number" -> "number";
                case "boolean" -> "boolean";
                case "object" -> "object";
                default -> "string";
            };
        }
        return "string";
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
