record PlanRequest(
    String goal,
    Map<String, Object> context,
    PlanningStrategy strategy,
    Map<String, Object> constraints
) {}
