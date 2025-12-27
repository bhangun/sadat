package tech.kayys.wayang.workflow.scheduler.dto;

public enum MissedExecutionStrategy {
    SKIP, // Skip missed execution
    RUN_IMMEDIATELY, // Run as soon as possible
    ALERT_ONLY // Just alert, don't run
}
