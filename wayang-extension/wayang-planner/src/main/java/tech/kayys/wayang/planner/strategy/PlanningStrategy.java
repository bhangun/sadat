package tech.kayys.wayang.planner.strategy;

import tech.kayys.wayang.planner.domain.*;
import io.smallrye.mutiny.Uni;
import java.util.List;

public interface PlanningStrategy {
    String name();
    Uni<List<Task>> decompose(Goal goal);
    Uni<List<Task>> revise(ExecutionPlan existing, List<Task> failed, PlanningContext context);
}
