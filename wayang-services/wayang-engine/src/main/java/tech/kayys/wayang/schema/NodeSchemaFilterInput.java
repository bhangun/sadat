package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class NodeSchemaFilterInput {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
