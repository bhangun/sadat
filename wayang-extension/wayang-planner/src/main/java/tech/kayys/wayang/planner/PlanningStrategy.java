public interface PlanningStrategy {
    ActionGraph plan(Goal goal, PlanningContext context);
}