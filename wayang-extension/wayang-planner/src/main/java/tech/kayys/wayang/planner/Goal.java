@Value
@Builder
public class Goal {
    String objective;
    List<Constraint> constraints;
    List<Requirement> requirements;
    Map<String, Object> parameters;
}