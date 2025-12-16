package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.model.ConnectionDefinition;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.NodeDefinition;
import tech.kayys.wayang.model.RuntimeConfig;
import tech.kayys.wayang.model.UIDefinition;

import org.jboss.logging.Logger;

import tech.kayys.wayang.model.ChangeType;
import tech.kayys.wayang.model.ConnectionChange;
import tech.kayys.wayang.model.DiffSummary;
import tech.kayys.wayang.model.LogicDiff;
import tech.kayys.wayang.model.NodeChange;
import tech.kayys.wayang.model.NodePositionChange;
import tech.kayys.wayang.model.RuntimeDiff;
import tech.kayys.wayang.model.UIDiff;
import tech.kayys.wayang.model.WorkflowDiff;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DiffService - Compare and diff workflows
 */
@ApplicationScoped
public class DiffService {

    private static final Logger LOG = Logger.getLogger(DiffService.class);

    /**
     * Compare two workflows and generate diff
     */
    public Uni<WorkflowDiff> diff(Workflow base, Workflow target) {
        LOG.debug(target);
        return Uni.createFrom().item(() -> {
            WorkflowDiff diff = new WorkflowDiff();
            diff.baseId = base.id.toString();
            diff.targetId = target.id.toString();
            diff.baseVersion = base.version;
            diff.targetVersion = target.version;

            // Compare logic
            LogicDiff logicDiff = compareLogic(base.logic, target.logic);
            diff.logicDiff = logicDiff;

            // Compare UI
            UIDiff uiDiff = compareUI(base.ui, target.ui);
            diff.uiDiff = uiDiff;

            // Compare runtime config
            RuntimeDiff runtimeDiff = compareRuntime(base.runtime, target.runtime);
            diff.runtimeDiff = runtimeDiff;

            // Generate summary
            diff.summary = generateSummary(logicDiff, uiDiff, runtimeDiff);

            return diff;
        });
    }

    /**
     * Compare logic definitions
     */
    private LogicDiff compareLogic(LogicDefinition base, LogicDefinition target) {
        LogicDiff diff = new LogicDiff();

        // Compare nodes
        diff.nodeChanges = compareNodes(base.nodes, target.nodes);

        // Compare connections
        diff.connectionChanges = compareConnections(base.connections, target.connections);

        // Compare rules
        diff.rulesChanged = !Objects.equals(base.rules, target.rules);

        return diff;
    }

    /**
     * Compare node lists
     */
    private List<NodeChange> compareNodes(List<NodeDefinition> baseNodes,
            List<NodeDefinition> targetNodes) {

        List<NodeChange> changes = new ArrayList<>();

        Map<String, NodeDefinition> baseMap = baseNodes.stream()
                .collect(Collectors.toMap(n -> n.id, n -> n));

        Map<String, NodeDefinition> targetMap = targetNodes.stream()
                .collect(Collectors.toMap(n -> n.id, n -> n));

        // Find added nodes
        for (NodeDefinition target : targetNodes) {
            if (!baseMap.containsKey(target.id)) {
                NodeChange change = new NodeChange();
                change.type = ChangeType.ADDED;
                change.nodeId = target.id;
                change.nodeName = target.name;
                change.nodeType = target.type;
                change.newValue = target;
                changes.add(change);
            }
        }

        // Find deleted nodes
        for (NodeDefinition base : baseNodes) {
            if (!targetMap.containsKey(base.id)) {
                NodeChange change = new NodeChange();
                change.type = ChangeType.DELETED;
                change.nodeId = base.id;
                change.nodeName = base.name;
                change.nodeType = base.type;
                change.oldValue = base;
                changes.add(change);
            }
        }

        // Find modified nodes
        for (NodeDefinition base : baseNodes) {
            NodeDefinition target = targetMap.get(base.id);
            if (target != null && !nodesEqual(base, target)) {
                NodeChange change = new NodeChange();
                change.type = ChangeType.MODIFIED;
                change.nodeId = base.id;
                change.nodeName = base.name;
                change.nodeType = base.type;
                change.oldValue = base;
                change.newValue = target;
                change.modifications = detectNodeModifications(base, target);
                changes.add(change);
            }
        }

        return changes;
    }

    /**
     * Compare connection lists
     */
    private List<ConnectionChange> compareConnections(
            List<ConnectionDefinition> baseConnections,
            List<ConnectionDefinition> targetConnections) {

        List<ConnectionChange> changes = new ArrayList<>();

        Map<String, ConnectionDefinition> baseMap = baseConnections.stream()
                .collect(Collectors.toMap(c -> c.id, c -> c));

        Map<String, ConnectionDefinition> targetMap = targetConnections.stream()
                .collect(Collectors.toMap(c -> c.id, c -> c));

        // Find added connections
        for (ConnectionDefinition target : targetConnections) {
            if (!baseMap.containsKey(target.id)) {
                ConnectionChange change = new ConnectionChange();
                change.type = ChangeType.ADDED;
                change.connectionId = target.id;
                change.from = target.from;
                change.to = target.to;
                change.newValue = target;
                changes.add(change);
            }
        }

        // Find deleted connections
        for (ConnectionDefinition base : baseConnections) {
            if (!targetMap.containsKey(base.id)) {
                ConnectionChange change = new ConnectionChange();
                change.type = ChangeType.DELETED;
                change.connectionId = base.id;
                change.from = base.from;
                change.to = base.to;
                change.oldValue = base;
                changes.add(change);
            }
        }

        // Find modified connections
        for (ConnectionDefinition base : baseConnections) {
            ConnectionDefinition target = targetMap.get(base.id);
            if (target != null && !connectionsEqual(base, target)) {
                ConnectionChange change = new ConnectionChange();
                change.type = ChangeType.MODIFIED;
                change.connectionId = base.id;
                change.from = base.from;
                change.to = base.to;
                change.oldValue = base;
                change.newValue = target;
                changes.add(change);
            }
        }

        return changes;
    }

    /**
     * Compare UI definitions
     */
    private UIDiff compareUI(UIDefinition base, UIDefinition target) {
        UIDiff diff = new UIDiff();

        if (base == null && target == null) {
            diff.changed = false;
            return diff;
        }

        if (base == null || target == null) {
            diff.changed = true;
            return diff;
        }

        // Compare canvas state
        diff.canvasChanged = !Objects.equals(base.canvas, target.canvas);

        // Compare node positions
        diff.nodePositionChanges = compareNodePositions(base.nodes, target.nodes);

        diff.changed = diff.canvasChanged || !diff.nodePositionChanges.isEmpty();

        return diff;
    }

    /**
     * Compare node positions
     */
    private List<NodePositionChange> compareNodePositions(
            List<UIDefinition.NodeUI> baseNodes,
            List<UIDefinition.NodeUI> targetNodes) {

        List<NodePositionChange> changes = new ArrayList<>();

        Map<String, UIDefinition.NodeUI> baseMap = baseNodes.stream()
                .collect(Collectors.toMap(n -> n.ref, n -> n));

        for (UIDefinition.NodeUI target : targetNodes) {
            UIDefinition.NodeUI base = baseMap.get(target.ref);
            if (base != null && !positionsEqual(base.position, target.position)) {
                NodePositionChange change = new NodePositionChange();
                change.nodeId = target.ref;
                change.oldPosition = base.position;
                change.newPosition = target.position;
                changes.add(change);
            }
        }

        return changes;
    }

    /**
     * Compare runtime configs
     */
    private RuntimeDiff compareRuntime(RuntimeConfig base, RuntimeConfig target) {
        RuntimeDiff diff = new RuntimeDiff();

        if (base == null && target == null) {
            diff.changed = false;
            return diff;
        }

        diff.changed = !Objects.equals(base, target);
        diff.modeChanged = base != null && target != null && base.mode != target.mode;
        diff.retriesChanged = !Objects.equals(
                base != null ? base.retryPolicy : null,
                target != null ? target.retryPolicy : null);

        return diff;
    }

    /**
     * Generate summary statistics
     */
    private DiffSummary generateSummary(LogicDiff logicDiff, UIDiff uiDiff, RuntimeDiff runtimeDiff) {
        DiffSummary summary = new DiffSummary();

        if (logicDiff != null) {
            summary.nodesAdded = (int) logicDiff.nodeChanges.stream()
                    .filter(c -> c.type == ChangeType.ADDED).count();
            summary.nodesDeleted = (int) logicDiff.nodeChanges.stream()
                    .filter(c -> c.type == ChangeType.DELETED).count();
            summary.nodesModified = (int) logicDiff.nodeChanges.stream()
                    .filter(c -> c.type == ChangeType.MODIFIED).count();

            summary.connectionsAdded = (int) logicDiff.connectionChanges.stream()
                    .filter(c -> c.type == ChangeType.ADDED).count();
            summary.connectionsDeleted = (int) logicDiff.connectionChanges.stream()
                    .filter(c -> c.type == ChangeType.DELETED).count();
            summary.connectionsModified = (int) logicDiff.connectionChanges.stream()
                    .filter(c -> c.type == ChangeType.MODIFIED).count();
        }

        summary.uiChanged = uiDiff != null && uiDiff.changed;
        summary.runtimeChanged = runtimeDiff != null && runtimeDiff.changed;

        summary.totalChanges = summary.nodesAdded + summary.nodesDeleted +
                summary.nodesModified + summary.connectionsAdded +
                summary.connectionsDeleted + summary.connectionsModified;

        return summary;
    }

    /**
     * Detect specific node modifications
     */
    private List<String> detectNodeModifications(NodeDefinition base, NodeDefinition target) {
        List<String> mods = new ArrayList<>();

        if (!Objects.equals(base.name, target.name)) {
            mods.add("name");
        }
        if (!Objects.equals(base.type, target.type)) {
            mods.add("type");
        }
        if (!Objects.equals(base.properties, target.properties)) {
            mods.add("properties");
        }
        if (!Objects.equals(base.inputs, target.inputs)) {
            mods.add("inputs");
        }
        if (!Objects.equals(base.outputs, target.outputs)) {
            mods.add("outputs");
        }

        return mods;
    }

    private boolean nodesEqual(NodeDefinition a, NodeDefinition b) {
        return Objects.equals(a.name, b.name) &&
                Objects.equals(a.type, b.type) &&
                Objects.equals(a.properties, b.properties) &&
                Objects.equals(a.inputs, b.inputs) &&
                Objects.equals(a.outputs, b.outputs);
    }

    private boolean connectionsEqual(ConnectionDefinition a, ConnectionDefinition b) {
        return Objects.equals(a.from, b.from) &&
                Objects.equals(a.to, b.to) &&
                Objects.equals(a.fromPort, b.fromPort) &&
                Objects.equals(a.toPort, b.toPort) &&
                Objects.equals(a.condition, b.condition);
    }

    private boolean positionsEqual(UIDefinition.Point a, UIDefinition.Point b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.x == b.x && a.y == b.y;
    }
}