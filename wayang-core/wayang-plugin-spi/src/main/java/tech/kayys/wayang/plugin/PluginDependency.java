package tech.kayys.wayang.plugin;

/**
 * Plugin Dependency
 */
public class PluginDependency {
    
    
    private String pluginId;
    
    
    private String versionRange; // semver range: "^1.0.0", ">=2.0.0 <3.0.0"
    
   
    private boolean required = true;
    
 
    private boolean optional = false;
    
    private String reason;
    
    public boolean versionMatches(String version) {
        // TODO: Implement semver range matching
        return true;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getVersionRange() {
        return versionRange;
    }

    public void setVersionRange(String versionRange) {
        this.versionRange = versionRange;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}