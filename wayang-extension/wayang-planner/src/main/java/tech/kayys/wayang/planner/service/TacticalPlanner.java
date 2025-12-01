package tech.kayys.wayang.planner.service;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.planner.domain.*;
import tech.kayys.wayang.planner.engine.NodeMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class TacticalPlanner {
    
    @Inject
    NodeMapper nodeMapper;
    
    @Inject
    @RestClient
    SchemaRegistryClient schemaRegistry;
    
    public Uni<ExecutionPlan> plan(StrategicPlan strategicPlan, PlanningContext context) {
        // Map each strategic task to concrete nodes
        return Multi.createFrom().iterable(strategicPlan.tasks())
            .onItem().transformToUniAndMerge(task -> 
                nodeMapper.mapToNodes(task, context)
            )
            .collect().asList()
            .map(nodeGroups -> {
                List<NodeDescriptor> allNodes = nodeGroups.stream()
                    .flatMap(List::stream)
                    .toList();
                
                // Build edges based on task dependencies
                List<Edge> edges = buildEdges(strategicPlan, allNodes);
                
                return new ExecutionPlan(
                    UUID.randomUUID().toString(),
                    strategicPlan.strategyUsed(),
                    allNodes,
                    edges,
                    buildMetadata(strategicPlan, context)
                );
            });
    }
    
    private List<Edge> buildEdges(StrategicPlan plan, List<NodeDescriptor> nodes) {
        Map<String, List<NodeDescriptor>> taskToNodes = new HashMap<>();
        
        // Group nodes by their source task
        nodes.forEach(node -> {
            String taskId = node.metadata().get("taskId").toString();
            taskToNodes.computeIfAbsent(taskId, k -> new ArrayList<>())
                .add(node);
        });
        
        List<Edge> edges = new ArrayList<>();
        
        // Connect nodes based on task dependencies
        plan.tasks().forEach(task -> {
            List<NodeDescriptor> currentNodes = taskToNodes.get(task.id());
            
            task.dependsOn().forEach(depTaskId -> {
                List<NodeDescriptor> dependentNodes = taskToNodes.get(depTaskId);
                
                if (dependentNodes != null && currentNodes != null) {
                    // Connect last node of dependency to first node of current
                    NodeDescriptor source = dependentNodes.get(dependentNodes.size() - 1);
                    NodeDescriptor target = currentNodes.get(0);
                    
                    edges.add(new Edge(source.id(), target.id(), EdgeType.SUCCESS));
                }
            });
        });
        
        return edges;
    }
    
    private Map<String, Object> buildMetadata(StrategicPlan plan, PlanningContext context) {
        return Map.of(
            "strategyUsed", plan.strategyUsed(),
            "goalDescription", plan.goal().description(),
            "createdAt", plan.createdAt(),
            "tenantId", context.tenantId(),
            "userId", context.userId()
        );
    }
}