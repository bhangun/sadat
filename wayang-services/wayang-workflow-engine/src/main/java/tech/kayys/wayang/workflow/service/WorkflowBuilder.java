package tech.kayys.wayang.workflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.execution.Variable;
import tech.kayys.wayang.schema.workflow.Trigger;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

/**
 * WorkflowDefinitionBuilder - Build workflows programmatically
 */
@ApplicationScoped
public class WorkflowBuilder {

    private static final Logger LOG = Logger.getLogger(WorkflowBuilder.class);

    private String id;
    private String name;
    private String description;
    private List<NodeDefinition> nodes = new ArrayList<>();
    private List<EdgeDefinition> edges = new ArrayList<>();
    private List<Variable> variables = new ArrayList<>();
    private List<Trigger> triggers = new ArrayList<>();

    public WorkflowBuilder() {
        this.id = UUID.randomUUID().toString();
    }

    public WorkflowBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public WorkflowBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public WorkflowBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public WorkflowBuilder addNode(NodeDefinition node) {
        this.nodes.add(node);
        return this;
    }

    public WorkflowBuilder addEdge(EdgeDefinition edge) {
        this.edges.add(edge);
        return this;
    }

    public WorkflowBuilder addVariable(Variable variable) {
        this.variables.add(variable);
        return this;
    }

    public WorkflowBuilder addTrigger(Trigger trigger) {
        this.triggers.add(trigger);
        return this;
    }

    /**
     * Create a simple linear workflow
     */
    public WorkflowBuilder createLinearWorkflow(List<NodeConfig> nodeConfigs) {
        int x = 100;
        NodeDefinition previousNode = null;

        for (int i = 0; i < nodeConfigs.size(); i++) {
            NodeConfig config = nodeConfigs.get(i);

            NodeDefinition node = new NodeDefinition();
            node.setId("node-" + i);
            node.setType(config.type);
            node.setDisplayName(config.name);

            nodes.add(node);

            // Connect to previous node
            if (previousNode != null) {
                EdgeDefinition edge = new EdgeDefinition();
                edge.setId("edge-" + (i - 1));
                edge.setFrom(previousNode.getId());
                edge.setTo(node.getId());
                edges.add(edge);
            }

            previousNode = node;
            x += 200;
        }

        return this;
    }

    /**
     * Add conditional branching
     */
    public WorkflowBuilder addConditionalBranch(String fromNodeId,
            String trueNodeId,
            String falseNodeId,
            String condition) {
        // Add condition node
        NodeDefinition condNode = new NodeDefinition();
        condNode.setId("cond-" + UUID.randomUUID().toString().substring(0, 8));
        condNode.setType("CONDITION");
        condNode.setDisplayName("Branch");

        nodes.add(condNode);

        // Add edges
        EdgeDefinition toCondition = new EdgeDefinition();
        toCondition.setId("edge-to-cond-" + condNode.getId());
        toCondition.setFrom(fromNodeId);
        toCondition.setTo(condNode.getId());
        edges.add(toCondition);

        EdgeDefinition trueEdge = new EdgeDefinition();
        trueEdge.setId("edge-true-" + condNode.getId());
        trueEdge.setFrom(condNode.getId());
        trueEdge.setTo(trueNodeId);
        edges.add(trueEdge);

        EdgeDefinition falseEdge = new EdgeDefinition();
        falseEdge.setId("edge-false-" + condNode.getId());
        falseEdge.setFrom(condNode.getId());
        falseEdge.setTo(falseNodeId);
        edges.add(falseEdge);

        return this;
    }

    /**
     * Add parallel execution
     */
    public WorkflowBuilder addParallelExecution(String fromNodeId,
            List<String> parallelNodeIds,
            String mergeNodeId) {
        // Add parallel node
        NodeDefinition parallelNode = new NodeDefinition();
        parallelNode.setId("parallel-" + UUID.randomUUID().toString().substring(0, 8));
        parallelNode.setType("PARALLEL");
        parallelNode.setDisplayName("Parallel");
        nodes.add(parallelNode);

        // Connect from node to parallel
        EdgeDefinition toParallel = new EdgeDefinition();
        toParallel.setId("edge-to-parallel");
        toParallel.setFrom(fromNodeId);
        toParallel.setTo(parallelNode.getId());
        edges.add(toParallel);

        // Connect parallel to all branches
        for (String nodeId : parallelNodeIds) {
            EdgeDefinition branch = new EdgeDefinition();
            branch.setId("edge-parallel-" + nodeId);
            branch.setFrom(parallelNode.getId());
            branch.setTo(nodeId);
            edges.add(branch);

            // Connect branch to merge
            EdgeDefinition toMerge = new EdgeDefinition();
            toMerge.setId("edge-merge-" + nodeId);
            toMerge.setFrom(nodeId);
            toMerge.setTo(mergeNodeId);
            edges.add(toMerge);
        }

        return this;
    }

    public WorkflowDefinition build() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(id);
        workflow.setName(name);
        workflow.setDescription(description);
        workflow.setNodes(nodes);
        workflow.setEdges(edges);
        workflow.setTriggers(triggers);

        return workflow;
    }

    public static class NodeConfig {
        String type;
        String name;
        Map<String, Object> nodeConfig;

        public NodeConfig(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public NodeConfig withConfig(Map<String, Object> config) {
            this.nodeConfig = config;
            return this;
        }
    }
}