@Value
@Builder
public class ReasoningStep {
    int stepNumber;
    String thought;          // Masked/safe version
    String action;
    Map<String, Object> observation;
    int tokens;
    Duration duration;
}
