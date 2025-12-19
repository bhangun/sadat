package tech.kayys.wayang.schema;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * UpdateWorkflowInput - Input for updating workflow
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateWorkflowInput {

    private String name;
    private String description;
    private LogicDefinitionDTO logic;
    private UIDefinitionDTO ui;
    private RuntimeConfigDTO runtime;
    private Set<String> tags;
    private Map<String, Object> metadata;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LogicDefinitionDTO getLogic() {
        return logic;
    }

    public void setLogic(LogicDefinitionDTO logic) {
        this.logic = logic;
    }

    public UIDefinitionDTO getUi() {
        return ui;
    }

    public void setUi(UIDefinitionDTO ui) {
        this.ui = ui;
    }

    public RuntimeConfigDTO getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeConfigDTO runtime) {
        this.runtime = runtime;
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    public void setTags(List<String> tags) {
        this.tags = new java.util.HashSet<>(tags);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
