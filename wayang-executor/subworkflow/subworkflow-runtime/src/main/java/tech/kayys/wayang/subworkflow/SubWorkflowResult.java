package tech.kayys.silat.executor.subworkflow;

import tech.kayys.silat.core.domain.RunStatus;

import java.time.Duration;
import java.util.Map;

/**
 * Sub-workflow result
 */
record SubWorkflowResult(
    RunStatus status,
    Map<String, Object> outputs,
    tech.kayys.silat.core.domain.ErrorInfo error,
    Duration duration
) {}