package tech.kayys.execution;

import java.util.List;
import java.util.Map;
import java.util.Optional;



public class ExecutionResult {
    Status status;
    Map<String, Object> outputs;
    List<Event> events;
    List<String> logs;
    ExecutionMetrics metrics;
    Optional<String> checkpointRef;
    Optional<String> error;
}