@Value
@Builder
public class ReasoningTrace {
    String nodeId;
    List<ReasoningStep> steps;
    int totalTokens;
    Duration totalTime;
}