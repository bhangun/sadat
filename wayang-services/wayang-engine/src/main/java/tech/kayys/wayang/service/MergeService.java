package tech.kayys.wayang.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.model.*;
import tech.kayys.wayang.model.ConnectionChange;
import tech.kayys.wayang.model.ConnectionDefinition;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.LogicDiff;
import tech.kayys.wayang.model.NodeChange;
import tech.kayys.wayang.model.NodeDefinition;
import tech.kayys.wayang.model.RuntimeConfig;
import tech.kayys.wayang.model.RuntimeDiff;
import tech.kayys.wayang.model.UIDefinition;
import tech.kayys.wayang.model.UIDiff;
import tech.kayys.wayang.model.WorkflowDiff;

/**
 * MergeService - Merge workflow changes
 */
@ApplicationScoped
public class MergeService {

    private static final Logger LOG = Logger.getLogger(MergeService.class);

    @Inject
    DiffService diffService;

    /**
     * Three-way merge: base + changes1 + changes2 -> merged
     */
    public Uni<MergeResult> merge(Workflow base, Workflow branch1, Workflow branch2) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Merging workflows: base=%s, branch1=%s, branch2=%s",
                    base.id, branch1.id, branch2.id);

            MergeResult result = new MergeResult();

            // Compute diffs
            WorkflowDiff diff1 = diffService.diff(base, branch1).await().indefinitely();
            WorkflowDiff diff2 = diffService.diff(base, branch2).await().indefinitely();

            // Merge logic
            LogicDefinition mergedLogic = mergeLogic(base.logic, diff1.logicDiff, diff2.logicDiff, result);

            // Merge UI
            UIDefinition mergedUI = mergeUI(base.ui, diff1.uiDiff, diff2.uiDiff, result);

            // Merge runtime
            RuntimeConfig mergedRuntime = mergeRuntime(base.runtime, diff1.runtimeDiff, diff2.runtimeDiff, result);

            // Build merged workflow
            if (!result.hasConflicts) {
                result.merged = new LogicDefinition();
                result.merged.nodes = mergedLogic.nodes;
                result.merged.connections = mergedLogic.connections;
                result.merged.rules = mergedLogic.rules;
                result.mergedUI = mergedUI;
                result.mergedRuntime = mergedRuntime;
            }

            return result;
        });
    }

    /**
     * Merge logic definitions
     */
    private LogicDefinition mergeLogic(LogicDefinition base, LogicDiff diff1, LogicDiff diff2, MergeResult result) {
        LogicDefinition merged = new LogicDefinition();
        merged.nodes = new ArrayList<>(base.nodes);
        merged.connections = new ArrayList<>(base.connections);
        merged.rules = new HashMap<>(base.rules);

        // Apply node changes from both branches
        Map<String, NodeDefinition> nodeMap = merged.nodes.stream()
                .collect(Collectors.toMap(n -> n.id, n -> n));

        // Process branch 1 changes
        for (NodeChange change : diff1.nodeChanges) {
            applyNodeChange(change, nodeMap, merged, result, "branch1");
        }

        // Process branch 2 changes
        for (NodeChange change : diff2.nodeChanges) {
            applyNodeChange(change, nodeMap, merged, result, "branch2");
        }

        merged.nodes = new ArrayList<>(nodeMap.values());

        // Merge connections
        mergeConnections(merged, diff1.connectionChanges, diff2.connectionChanges, result);

        return merged;
    }

    /**
     * Apply node change with conflict detection
     */
    private void applyNodeChange(NodeChange change, Map<String, NodeDefinition> nodeMap,
            LogicDefinition merged, MergeResult result, String branch) {

        switch (change.type) {
            case ADDED -> {
                if (nodeMap.containsKey(change.nodeId)) {
                    // Conflict: both branches added same node ID
                    result.addConflict(new MergeConflict(
                            ConflictType.NODE_ADDED_BOTH,
                            change.nodeId,
                            "Node added in both branches",
                            branch));
                } else {
                    nodeMap.put(change.nodeId, change.newValue);
                }
            }
            case DELETED -> {
                if (!nodeMap.containsKey(change.nodeId)) {
                    // Already deleted or not present
                    LOG.debugf("Node %s already deleted", change.nodeId);
                } else {
                    nodeMap.remove(change.nodeId);
                }
            }
            case MODIFIED -> {
                NodeDefinition current = nodeMap.get(change.nodeId);
                if (current == null) {
                    // Node was deleted in other branch
                    result.addConflict(new MergeConflict(
                            ConflictType.NODE_MODIFIED_DELETED,
                            change.nodeId,
                            "Node modified in one branch, deleted in other",
                            branch));
                } else if (!nodesEqual(current, change.oldValue)) {
                    // Modified in both branches
                    result.addConflict(new MergeConflict(
                            ConflictType.NODE_MODIFIED_BOTH,
                            change.nodeId,
                            "Node modified in both branches",
                            branch));
                } else {
                    nodeMap.put(change.nodeId, change.newValue);
                }
            }
        }
    }

    /**
     * Merge connections
     */
    private void mergeConnections(LogicDefinition merged,
            List<ConnectionChange> changes1, List<ConnectionChange> changes2, MergeResult result) {

        Map<String, ConnectionDefinition> connMap = merged.connections.stream()
                .collect(Collectors.toMap(c -> c.id, c -> c));

        // Apply changes from both branches
        for (ConnectionChange change : changes1) {
            applyConnectionChange(change, connMap, result, "branch1");
        }

        for (ConnectionChange change : changes2) {
            applyConnectionChange(change, connMap, result, "branch2");
        }

        merged.connections = new ArrayList<>(connMap.values());
    }

    /**
     * Apply connection change
     */
    private void applyConnectionChange(ConnectionChange change,
            Map<String, ConnectionDefinition> connMap, MergeResult result, String branch) {

        switch (change.type) {
            case ADDED -> {
                if (connMap.containsKey(change.connectionId)) {
                    result.addConflict(new MergeConflict(
                            ConflictType.CONNECTION_ADDED_BOTH,
                            change.connectionId,
                            "Connection added in both branches",
                            branch));
                } else {
                    connMap.put(change.connectionId, change.newValue);
                }
            }
            case DELETED -> {
                connMap.remove(change.connectionId);
            }
            case MODIFIED -> {
                ConnectionDefinition current = connMap.get(change.connectionId);
                if (current == null) {
                    result.addConflict(new MergeConflict(
                            ConflictType.CONNECTION_MODIFIED_DELETED,
                            change.connectionId,
                            "Connection modified in one branch, deleted in other",
                            branch));
                } else {
                    connMap.put(change.connectionId, change.newValue);
                }
            }
        }
    }

    /**
     * Merge UI definitions
     */
    private UIDefinition mergeUI(UIDefinition base, UIDiff diff1, UIDiff diff2, MergeResult result) {
        // For UI, last-write-wins for canvas state
        // Merge node positions by taking most recent change
        UIDefinition merged = new UIDefinition();
        if (base != null) {
            merged.canvas = base.canvas;
            merged.nodes = new ArrayList<>(base.nodes);
            merged.connections = new ArrayList<>(base.connections);
        }
        return merged;
    }

    /**
     * Merge runtime configs
     */
    private RuntimeConfig mergeRuntime(RuntimeConfig base, RuntimeDiff diff1, RuntimeDiff diff2, MergeResult result) {
        // For runtime config, prefer explicit changes
        // If both changed, create conflict
        if (diff1.changed && diff2.changed) {
            result.addConflict(new MergeConflict(
                    ConflictType.RUNTIME_MODIFIED_BOTH,
                    "runtime",
                    "Runtime config modified in both branches",
                    "both"));
        }
        return base;
    }

    private boolean nodesEqual(NodeDefinition a, NodeDefinition b) {
        return Objects.equals(a.name, b.name) &&
                Objects.equals(a.type, b.type) &&
                Objects.equals(a.properties, b.properties);
    }
}