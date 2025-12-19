package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonInclude;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublishRequest {
    private String version;
    private String description;
    private boolean active;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
