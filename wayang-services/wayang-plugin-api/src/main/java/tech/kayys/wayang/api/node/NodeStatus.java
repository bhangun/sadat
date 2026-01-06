package tech.kayys.wayang.api.node;

public enum NodeStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    SKIPPED,
    TIMEOUT
}
