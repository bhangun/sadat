package tech.kayys.wayang.plugin.node;

public enum NodeStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    SKIPPED,
    TIMEOUT
}
