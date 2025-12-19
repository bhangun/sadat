package tech.kayys.wayang.schema;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * SelectionPayload - User selection
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelectionPayload {

    private String userId;
    private Set<String> selectedNodeIds = new HashSet<>();
    private Set<String> selectedConnectionIds = new HashSet<>();

    // Getters and setters...
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<String> getSelectedNodeIds() {
        return selectedNodeIds;
    }

    public void setSelectedNodeIds(Set<String> selectedNodeIds) {
        this.selectedNodeIds = selectedNodeIds;
    }

    public Set<String> getSelectedConnectionIds() {
        return selectedConnectionIds;
    }

    public void setSelectedConnectionIds(Set<String> selectedConnectionIds) {
        this.selectedConnectionIds = selectedConnectionIds;
    }
}
