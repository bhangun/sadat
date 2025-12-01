package tech.kayys.wayang.planner.service;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.planner.domain.*;
import tech.kayys.wayang.planner.strategy.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;

@ApplicationScoped
public class StrategicPlanner {
    
    @Inject
    TaskDecomposer decomposer;
    
    @Inject
    Map<String, PlanningStrategy> strategies;
    
    public Uni<StrategicPlan> plan(PlanningContext context) {
        // Select planning strategy based on goal complexity
        PlanningStrategy strategy = selectStrategy(context);
        
        return strategy.decompose(context.goal())
            .map(tasks -> new StrategicPlan(
                UUID.randomUUID().toString(),
                context.goal(),
                tasks,
                strategy.name(),
                Instant.now()
            ));
    }
    
    public Uni<StrategicPlan> replan(ExecutionPlan existing, PlanningContext context) {
        // Analyze what went wrong
        List<Task> failedTasks = identifyFailedTasks(existing, context);
        
        // Adjust strategy if needed
        PlanningStrategy strategy = selectStrategy(context);
        
        return strategy.revise(existing, failedTasks, context)
            .map(tasks -> new StrategicPlan(
                UUID.randomUUID().toString(),
                context.goal(),
                tasks,
                strategy.name(),
                Instant.now()
            ));
    }
    
    private PlanningStrategy selectStrategy(PlanningContext context) {
        // Simple heuristic - can be made more sophisticated
        if (context.complexity() > 0.8) {
            return strategies.get("tree-of-thought");
        } else if (context.requiresInteraction()) {
            return strategies.get("react");
        } else {
            return strategies.get("chain-of-thought");
        }
    }
    
    private List<Task> identifyFailedTasks(ExecutionPlan plan, PlanningContext context) {
        return context.failedNodes().stream()
            .map(nodeId -> plan.findTaskForNode(nodeId))
            .filter(Objects::nonNull)
            .toList();
    }
}