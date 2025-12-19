package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonInclude;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeSchemaDTO {
    private String id;
    private String name;
    // Add fields as needed

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
