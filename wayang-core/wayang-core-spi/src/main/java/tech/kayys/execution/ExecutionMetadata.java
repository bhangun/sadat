package tech.kayys.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class ExecutionMetadata {
    Instant startTime;
    Instant endTime;
    Duration duration;
    int attempts;
    String executorId;
    String poolId;
    Map<String, String> tags;
}
