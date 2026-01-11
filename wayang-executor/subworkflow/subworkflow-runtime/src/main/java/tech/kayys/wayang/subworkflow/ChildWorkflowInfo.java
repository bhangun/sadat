package tech.kayys.silat.api.subworkflow;

import tech.kayys.silat.core.domain.WorkflowRunId;
import tech.kayys.silat.core.domain.NodeId;
import tech.kayys.silat.core.domain.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record ChildWorkflowInfo(
    String runId,
    String parentNodeId,
    String workflowDefinitionId,
    String status,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    String tenantId
) {}