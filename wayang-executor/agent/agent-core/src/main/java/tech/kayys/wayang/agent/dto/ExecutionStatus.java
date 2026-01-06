package tech.kayys.wayang.agent.dto;



/**
 * Execution Status
 */
public enum ExecutionStatus {
    PENDING,
    PLANNING,
    EXECUTING,
    SUCCESS,
    FAILED,
    TIMEOUT,
    CANCELLED,
    PARTIAL_SUCCESS
}