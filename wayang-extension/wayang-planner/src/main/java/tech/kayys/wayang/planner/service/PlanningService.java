package tech.kayys.wayang.planner.service;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.planner.domain.*;
import tech.kayys.wayang.planner.engine.*;
import tech.kayys.wayang.planner.strategy.PlanningStrategy;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class PlanningService {
    
    private static final Logger LOG = Logger.getLogger(PlanningService.class);
    
    @Inject
    GoalParser goalParser;
    
    @Inject
    TaskDecomposer decomposer;
    
    @Inject
    NodeMapper nodeMapper;
    
    @Inject
    PlanValidator planValidator;
    
    @Inject
    StrategicPlanner strategicPlanner;
    
    @Inject
    TacticalPlanner tacticalPlanner;
    
    @Inject
    ContextFetcher contextFetcher;
    
    public Uni<ExecutionPlan> createPlan(
        String tenantId, 
        String userId, 
        PlanRequest request
    ) {
        LOG.infof("Creating plan for goal: %s", request.goal());
        
        return goalParser.parse(request.goal())
            .flatMap(goal -> contextFetcher.fetchContext(tenantId, goal)
                .flatMap(context -> {
                    PlanningContext planContext = new PlanningContext(
                        tenantId, userId, goal, context, request.constraints()
                    );
                    
                    // Strategic planning: high-level task breakdown
                    return strategicPlanner.plan(planContext)
                        .flatMap(strategicPlan -> 
                            // Tactical planning: map to concrete nodes
                            tacticalPlanner.plan(strategicPlan, planContext)
                        );
                })
            )
            .flatMap(plan -> planValidator.validate(plan)
                .map(validationResult -> {
                    if (!validationResult.isValid()) {
                        throw new ValidationException(
                            "Plan validation failed", 
                            validationResult.errors()
                        );
                    }
                    return plan;
                })
            )
            .invoke(plan -> LOG.infof("Plan created: %s", plan.planId()));
    }
    
    public Uni<ValidationResult> validatePlan(String tenantId, String planId) {
        return planRepository.findByIdAndTenant(planId, tenantId)
            .flatMap(plan -> {
                if (plan == null) {
                    return Uni.createFrom().failure(
                        new NotFoundException("Plan not found")
                    );
                }
                return planValidator.validate(plan);
            });
    }
    
    public Uni<ExecutionPlan> revisePlan(
        String tenantId, 
        String planId, 
        RevisionRequest revision
    ) {
        LOG.infof("Revising plan %s based on feedback", planId);
        
        return planRepository.findByIdAndTenant(planId, tenantId)
            .flatMap(existingPlan -> {
                // Use feedback to guide replanning
                PlanningContext revisedContext = existingPlan.context()
                    .withFeedback(revision.feedback())
                    .withFailedNodes(revision.failedNodes());
                
                return strategicPlanner.replan(existingPlan, revisedContext)
                    .flatMap(strategicPlan -> 
                        tacticalPlanner.plan(strategicPlan, revisedContext)
                    );
            });
    }
}