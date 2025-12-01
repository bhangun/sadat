package tech.kayys.wayang.planner;

public interface PlannerEngine {
    ExecutionPlan plan(PlanningRequest request);
    ExecutionPlan replan(ExecutionPlan currentPlan, ReplanRequest request);
    PlanValidation validate(ExecutionPlan plan);
    PlanEstimate estimate(ExecutionPlan plan);
}