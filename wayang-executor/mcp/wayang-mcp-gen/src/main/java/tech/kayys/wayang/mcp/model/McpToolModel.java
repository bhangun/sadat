package tech.kayys.wayang.mcp.model;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import java.util.Map;

public class McpToolModel {
    private String name;
    private String description;
    private String path;
    private String method;
    private String operationId;
    private String summary;
    private List<McpParameterModel> parameters;
    private Map<String, String> responseTypes;
    private List<SecurityRequirement> securityRequirements;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<McpParameterModel> getParameters() {
        return parameters;
    }

    public void setParameters(List<McpParameterModel> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(Map<String, String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<SecurityRequirement> getSecurityRequirements() {
        return securityRequirements;
    }

    public void setSecurityRequirements(List<SecurityRequirement> securityRequirements) {
        this.securityRequirements = securityRequirements;
    }

    public String getClassName() {
        if (name == null || name.isEmpty()) {
            return "UnknownTool";
        }
        // Capitalize first letter and append "Tool"
        return name.substring(0, 1).toUpperCase() + name.substring(1) + "Tool";
    }
}
