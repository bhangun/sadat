@Value
@Builder
public class ExecutionMetadata {
    Instant startTime;
    Instant endTime;
    Duration duration;
    int attempts;
    String executorId;
    String poolId;
    Map<String, String> tags;
}
