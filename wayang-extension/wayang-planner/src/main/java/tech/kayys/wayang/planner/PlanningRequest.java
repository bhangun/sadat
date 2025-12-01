
// Planning Components
@Value
@Builder
public class PlanningRequest {
    String intent;              // Natural language or structured goal
    UUID tenantId;
    String userId;
    Map<String, Object> context;
    PlanningHints hints;
}
