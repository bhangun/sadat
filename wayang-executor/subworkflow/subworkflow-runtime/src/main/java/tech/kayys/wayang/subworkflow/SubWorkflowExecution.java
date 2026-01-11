package tech.kayys.silat.executor.subworkflow;

import tech.kayys.silat.core.domain.ErrorInfo;
import tech.kayys.silat.core.domain.NodeId;
import tech.kayys.silat.core.domain.WorkflowRunId;

import java.time.Instant;
import java.util.Map;

/**
 * Sub-workflow execution tracking
 */
record SubWorkflowExecution(
    WorkflowRunId parentRunId,
    NodeId parentNodeId,
    WorkflowRunId childRunId,
    SubWorkflowConfig config,
    Map<String, Object> childInputs,
    String targetTenantId,
    Instant startedAt,
    SubWorkflowStatus status,
    SubWorkflowResult result,
    ErrorInfo error
) {
    // Constructor with defaults
    SubWorkflowExecution(
            WorkflowRunId parentRunId,
            NodeId parentNodeId,
            WorkflowRunId childRunId,
            SubWorkflowConfig config,
            Map<String, Object> childInputs,
            String targetTenantId,
            Instant startedAt,
            SubWorkflowStatus status) {
        this(parentRunId, parentNodeId, childRunId, config, childInputs,
            targetTenantId, startedAt, status, null, null);
    }

    SubWorkflowExecution withChildRunId(WorkflowRunId childRunId) {
        return new SubWorkflowExecution(parentRunId, parentNodeId, childRunId,
            config, childInputs, targetTenantId, startedAt, status, result, error);
    }

    SubWorkflowExecution withResult(SubWorkflowResult result) {
        return new SubWorkflowExecution(parentRunId, parentNodeId, childRunId,
            config, childInputs, targetTenantId, startedAt,
            SubWorkflowStatus.COMPLETED, result, error);
    }

    SubWorkflowExecution withError(ErrorInfo error) {
        return new SubWorkflowExecution(parentRunId, parentNodeId, childRunId,
            config, childInputs, targetTenantId, startedAt, status, result, error);
    }
}