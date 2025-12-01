@Value
@Builder
public class PoolMetrics {
    int totalWorkers;
    int activeWorkers;
    int queuedTasks;
    double utilizationPercent;
    Duration avgTaskDuration;
}