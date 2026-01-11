package tech.kayys.wayang.mcp.model;

import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;

public class ApiSpecification {
    private String title;
    private String description;
    private String version;
    private String baseUrl;
    private List<ApiOperation> operations;
    private Map<String, SecurityScheme> securitySchemes;

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<ApiOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<ApiOperation> operations) {
        this.operations = operations;
    }

    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = securitySchemes;
    }
}
