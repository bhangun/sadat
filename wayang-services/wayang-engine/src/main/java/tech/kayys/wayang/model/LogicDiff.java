package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * LogicDiff - Changes in workflow logic (nodes, connections, rules)
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogicDiff {

    public List<NodeChange> nodeChanges = new ArrayList<>();
    public List<ConnectionChange> connectionChanges = new ArrayList<>();
    public boolean rulesChanged = false;
    public RulesDiff rulesDiff;

    /**
     * Check if logic has any changes
     */
    public boolean hasChanges() {
        return !nodeChanges.isEmpty() ||
                !connectionChanges.isEmpty() ||
                rulesChanged;
    }

    /**
     * Count total changes
     */
    public int totalChanges() {
        return nodeChanges.size() + connectionChanges.size() +
                (rulesChanged ? 1 : 0);
    }
}
