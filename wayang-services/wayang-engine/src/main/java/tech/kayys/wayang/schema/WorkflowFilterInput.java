package tech.kayys.wayang.schema;

import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * WorkflowFilterInput - Filter for listing workflows
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowFilterInput {

    private String search; // Search in name/description
    private WorkflowStatus status;
    private Set<String> tags;
    private String createdBy;
    private Instant createdAfter;
    private Instant createdBefore;

    // Getters and setters...
    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
