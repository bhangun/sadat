package tech.kayys.wayang.schema.governance;

import java.util.List;
import java.util.Map;

public class DataContract {
    private String id;
    private String displayName;
    private String description;
    private String type;
    private Object jsonSchema;
    private Map<String, Object> constraints;
    private List<Map<String, Object>> transformations;
    private String version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(Object jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public List<Map<String, Object>> getTransformations() {
        return transformations;
    }

    public void setTransformations(List<Map<String, Object>> transformations) {
        this.transformations = transformations;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
