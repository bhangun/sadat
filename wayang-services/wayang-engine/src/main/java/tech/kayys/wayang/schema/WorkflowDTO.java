package tech.kayys.wayang.schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tech.kayys.wayang.model.ValidationResult.ValidationError;

/**
 * WorkflowDTO - Primary workflow data transfer object
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowDTO {

    @NotNull
    private String id;

    @NotBlank
    private String version;

    @NotBlank
    @Size(min = 3, max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    private WorkflowStatus status;

    @NotNull
    private Instant createdAt;

    @NotNull
    private Instant updatedAt;

    @NotBlank
    private String createdBy;

    @NotBlank
    private String tenantId;

    // Components
    private LogicDefinitionDTO logic;
    private UIDefinitionDTO ui;
    private RuntimeConfigDTO runtime;

    // Metadata
    private Set<String> tags = new HashSet<>();
    private Map<String, Object> metadata = new HashMap<>();

    // Validation
    private ValidationStatus validationStatus;
    private List<ValidationError> validationErrors = new ArrayList<>();

    // Audit
    private String lastModifiedBy;
    private String publishedBy;
    private Instant publishedAt;

    // Getters and setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

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

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }
}