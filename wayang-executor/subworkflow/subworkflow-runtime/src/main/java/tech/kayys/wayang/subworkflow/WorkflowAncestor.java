package tech.kayys.silat.api.subworkflow;

record WorkflowAncestor(
    String runId,
    String workflowDefinitionId,
    String status,
    int level
) {}