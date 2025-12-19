package tech.kayys.wayang.schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CreateWorkflowInput - Input for creating new workflow
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateWorkflowInput {

    @NotBlank
    @Size(min = 3, max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private LogicDefinitionDTO logic;
    private UIDefinitionDTO ui;
    private RuntimeConfigDTO runtime;

    private Set<String> tags = new HashSet<>();
    private Map<String, Object> metadata = new HashMap<>();

    // Getters and setters...
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}