package tech.kayys.wayang.workflow.model.saga;

enum CompensationType {
    FORWARD_RECOVERY, // Retry failed operations
    PARTIAL_ROLLBACK, // Compensate some nodes
    FULL_ROLLBACK // Compensate all nodes
}
