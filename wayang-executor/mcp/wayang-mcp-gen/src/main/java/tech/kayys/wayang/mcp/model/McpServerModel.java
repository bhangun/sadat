package tech.kayys.wayang.mcp.model;

import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;

public class McpServerModel {
    private String packageName;
    private String serverName;
    private String serverClass;
    private String title;
    private String description;
    private String version;
    private String baseUrl;
    private boolean includeAuth;
    private List<McpToolModel> tools;
    private Map<String, SecurityScheme> securitySchemes;

    // Getters and setters
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerClass() {
        return serverClass;
    }

    public void setServerClass(String serverClass) {
        this.serverClass = serverClass;
    }

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

    public boolean isIncludeAuth() {
        return includeAuth;
    }

    public void setIncludeAuth(boolean includeAuth) {
        this.includeAuth = includeAuth;
    }

    public List<McpToolModel> getTools() {
        return tools;
    }

    public void setTools(List<McpToolModel> tools) {
        this.tools = tools;
    }

    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = securitySchemes;
    }

    public String getPackagePath() {
        return packageName.replace(".", "/");
    }
}