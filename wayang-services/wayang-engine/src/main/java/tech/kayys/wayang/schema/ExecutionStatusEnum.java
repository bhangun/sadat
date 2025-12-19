package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ExecutionStatusEnum - Execution status
 */
@RegisterForReflection
public enum ExecutionStatusEnum {
    PENDING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMEOUT
}
