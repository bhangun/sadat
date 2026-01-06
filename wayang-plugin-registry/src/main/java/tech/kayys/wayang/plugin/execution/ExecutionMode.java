package tech.kayys.wayang.plugin.execution;

public enum ExecutionMode {
    SYNC, // Blocking execution
    ASYNC, // Non-blocking with callback
    STREAM // Streaming results
}
