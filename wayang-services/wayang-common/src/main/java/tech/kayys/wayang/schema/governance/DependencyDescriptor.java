package tech.kayys.wayang.schema.governance;

import java.util.Arrays;
import java.util.List;

public class DependencyDescriptor {
    private String id;
    private String version;
    private Boolean optional = false;
    private String scope = "runtime";

    public DependencyDescriptor() {
    }

    public DependencyDescriptor(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Dependency ID cannot be empty");
        }
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Dependency version cannot be empty");
        }
        if (!version.matches("^[\\^~]?\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?$")) {
            throw new IllegalArgumentException("Invalid dependency version format");
        }
        this.version = version;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        List<String> validScopes = Arrays.asList("runtime", "compile", "test");
        if (!validScopes.contains(scope)) {
            throw new IllegalArgumentException("Invalid scope: " + scope);
        }
        this.scope = scope;
    }
}
