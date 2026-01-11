package tech.kayys.wayang.mcp.model;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import java.util.Map;

public class ApiOperation {
    private String path;
    private String method;
    private String operationId;
    private String summary;
    private String description;
    private List<ApiParameter> parameters;
    private Map<String, String> responseTypes;
    private List<SecurityRequirement> securityRequirements;

    // Getters and setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ApiParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ApiParameter> parameters) {
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
}
