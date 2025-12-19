package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Run context for caching
 */
public class RunContext {
    final WorkflowRun run;
    final Instant cachedAt;

    public RunContext(WorkflowRun run) {
        this.run = run;
        this.cachedAt = Instant.now();
    }
}