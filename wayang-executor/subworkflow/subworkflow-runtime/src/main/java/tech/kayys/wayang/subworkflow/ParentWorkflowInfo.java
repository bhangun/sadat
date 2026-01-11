package tech.kayys.silat.api.subworkflow;

import tech.kayys.silat.core.domain.WorkflowRunId;
import tech.kayys.silat.core.domain.NodeId;
import tech.kayys.silat.core.domain.TenantId;

record ParentWorkflowInfo(
    String runId,
    String parentNodeId,
    String workflowDefinitionId,
    String status,
    String tenantId
) {}