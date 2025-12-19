package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WorkflowInput {
    private String json;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
