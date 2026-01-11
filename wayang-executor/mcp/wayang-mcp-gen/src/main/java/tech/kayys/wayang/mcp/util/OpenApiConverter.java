package tech.kayys.wayang.mcp.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.servers.Server;
import tech.kayys.wayang.mcp.model.ApiOperation;
import tech.kayys.wayang.mcp.model.ApiParameter;
import tech.kayys.wayang.mcp.model.ApiSpecification;

import java.util.*;
import java.util.stream.Collectors;

public class OpenApiConverter {

    public static ApiSpecification convertToApiSpec(OpenAPI openAPI) {
        ApiSpecification apiSpec = new ApiSpecification();

        // Convert info
        if (openAPI.getInfo() != null) {
            Info info = openAPI.getInfo();
            apiSpec.setTitle(info.getTitle());
            apiSpec.setDescription(info.getDescription());
            apiSpec.setVersion(info.getVersion());
        }

        // Extract base URL
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            Server firstServer = openAPI.getServers().get(0);
            apiSpec.setBaseUrl(firstServer.getUrl());
        }

        // Convert security schemes
        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null) {
            apiSpec.setSecuritySchemes(openAPI.getComponents().getSecuritySchemes());
        }

        // Convert paths to operations
        List<ApiOperation> operations = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                operations.addAll(convertPathItem(path, pathItem));
            });
        }

        apiSpec.setOperations(operations);
        return apiSpec;
    }

    private static List<ApiOperation> convertPathItem(String path, PathItem pathItem) {
        List<ApiOperation> operations = new ArrayList<>();

        Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();
        operationMap.forEach((method, operation) -> {
            ApiOperation apiOp = convertOperation(path, method.name(), operation);
            operations.add(apiOp);
        });

        return operations;
    }

    private static ApiOperation convertOperation(String path, String method, Operation operation) {
        ApiOperation apiOp = new ApiOperation();

        apiOp.setPath(path);
        apiOp.setMethod(method);
        apiOp.setOperationId(operation.getOperationId());
        apiOp.setSummary(operation.getSummary());
        apiOp.setDescription(operation.getDescription());

        // Convert parameters
        List<ApiParameter> parameters = new ArrayList<>();
        if (operation.getParameters() != null) {
            parameters.addAll(operation.getParameters().stream()
                    .map(OpenApiConverter::convertParameter)
                    .collect(Collectors.toList()));
        }

        // Convert request body
        if (operation.getRequestBody() != null) {
            parameters.addAll(convertRequestBody(operation.getRequestBody()));
        }

        apiOp.setParameters(parameters);

        // Convert responses
        Map<String, String> responseTypes = new HashMap<>();
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((code, response) -> {
                responseTypes.put(code, response.getDescription());
            });
        }
        apiOp.setResponseTypes(responseTypes);

        // Convert security requirements
        if (operation.getSecurity() != null) {
            apiOp.setSecurityRequirements(operation.getSecurity());
        }

        return apiOp;
    }

    private static ApiParameter convertParameter(Parameter param) {
        ApiParameter apiParam = new ApiParameter();

        apiParam.setName(param.getName());
        apiParam.setIn(param.getIn());
        apiParam.setDescription(param.getDescription());
        apiParam.setRequired(param.getRequired() != null ? param.getRequired() : false);

        if (param.getSchema() != null) {
            apiParam.setType(getSchemaType(param.getSchema()));
        } else {
            apiParam.setType("string");
        }

        if (param.getExample() != null) {
            apiParam.setExample(param.getExample().toString());
        }

        return apiParam;
    }

    private static List<ApiParameter> convertRequestBody(RequestBody requestBody) {
        List<ApiParameter> parameters = new ArrayList<>();

        if (requestBody.getContent() != null) {
            MediaType jsonContent = requestBody.getContent().get("application/json");
            if (jsonContent != null && jsonContent.getSchema() != null) {
                Schema schema = jsonContent.getSchema();

                if (schema.getProperties() != null) {
                    schema.getProperties().forEach((propName, propSchema) -> {
                        ApiParameter param = new ApiParameter();
                        param.setName((String) propName);
                        param.setIn("body");
                        param.setType(getSchemaType((Schema) propSchema));
                        param.setDescription(((Schema) propSchema).getDescription());
                        param.setRequired(schema.getRequired() != null &&
                                schema.getRequired().contains(propName));
                        parameters.add(param);
                    });
                } else {
                    ApiParameter bodyParam = new ApiParameter();
                    bodyParam.setName("requestBody");
                    bodyParam.setIn("body");
                    bodyParam.setType("object");
                    bodyParam.setDescription(requestBody.getDescription());
                    bodyParam.setRequired(requestBody.getRequired() != null ? requestBody.getRequired() : false);
                    parameters.add(bodyParam);
                }
            }
        }

        return parameters;
    }

    private static String getSchemaType(Schema schema) {
        if (schema == null)
            return "string";

        String type = schema.getType();
        if (type != null) {
            switch (type.toLowerCase()) {
                case "integer":
                    return schema.getFormat() != null && schema.getFormat().equals("int64") ? "long" : "int";
                case "number":
                    return schema.getFormat() != null && schema.getFormat().equals("float") ? "float" : "double";
                case "boolean":
                    return "boolean";
                case "array":
                    Schema items = schema.getItems();
                    String itemType = items != null ? getSchemaType(items) : "string";
                    return "List<" + itemType + ">";
                case "object":
                    return "object";
                default:
                    return "string";
            }
        }

        return "string";
    }
}
