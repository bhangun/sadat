package tech.kayys.silat.api.subworkflow;

record WorkflowDescendant(
    String runId,
    String workflowDefinitionId,
    String status,
    int depth
) {}