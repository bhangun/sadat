package tech.kayys.silat.api.subworkflow;

import java.util.List;

record WorkflowHierarchyNode(
    String runId,
    String workflowDefinitionId,
    String status,
    int depth,
    List<WorkflowHierarchyNode> children
) {}