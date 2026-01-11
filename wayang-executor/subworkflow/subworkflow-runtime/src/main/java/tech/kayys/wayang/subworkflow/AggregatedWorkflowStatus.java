package tech.kayys.silat.api.subworkflow;

import java.util.Map;

record AggregatedWorkflowStatus(
    String rootRunId,
    String rootStatus,
    int totalWorkflows,
    Map<String, Integer> statusCounts
) {}