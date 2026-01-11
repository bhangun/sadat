package tech.kayys.wayang.mcp.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import tech.kayys.wayang.mcp.dto.AuthLocation;

@Embeddable
public class AuthConfig {
    @Enumerated(EnumType.STRING)
    private AuthLocation location;
    
    private String paramName;
    private String scheme; // e.g., "Bearer", "ApiKey"
    
    // Getters and setters
    public AuthLocation getLocation() {
        return location;
    }
    
    public void setLocation(AuthLocation location) {
        this.location = location;
    }
    
    public String getParamName() {
        return paramName;
    }
    
    public void setParamName(String paramName) {
        this.paramName = paramName;
    }
    
    public String getScheme() {
        return scheme;
    }
    
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
}