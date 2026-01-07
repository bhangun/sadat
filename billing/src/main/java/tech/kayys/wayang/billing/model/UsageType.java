package tech.kayys.wayang.billing.model;

 /**
 * Usage types
 */
public enum UsageType {
    WORKFLOW_EXECUTION,
    AI_TOKEN_USAGE,
    API_CALL,
    STORAGE_GB_HOUR,
    DATA_TRANSFER,
    COMPUTE_HOUR,
    HUMAN_TASK,
    INTEGRATION_CALL,
    CUSTOM_METRIC
}
