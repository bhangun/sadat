package tech.kayys.silat.executor.subworkflow;

/**
 * Sub-workflow status
 */
enum SubWorkflowStatus {
    INITIALIZING,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMEOUT,
    DETACHED
}