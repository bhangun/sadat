package tech.kayys.wayang.agent;

/**
 * Built-in Executor - Executes agent coordination
 */
public record BuiltInExecutor(
    ExecutionMode mode,
    int maxParallelTasks,
    boolean enableFailover
) {
    
    public static BuiltInExecutor createDefault() {
        return new BuiltInExecutor(
            ExecutionMode.ADAPTIVE,
            5,
            true
        );
    }
}
