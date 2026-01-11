package tech.kayys.silat.executor.subworkflow;

import java.time.Duration;
import java.util.Map;

/**
 * Sub-workflow configuration
 */
record SubWorkflowConfig(
    String workflowId,
    String workflowName,
    Map<String, String> inputMapping,
    Map<String, String> outputMapping,
    boolean waitForCompletion,
    Duration timeout,
    String targetTenantId,
    SubWorkflowErrorStrategy errorStrategy,
    boolean enableCompensation,
    boolean passThroughContext,
    Map<String, String> labels
) {}