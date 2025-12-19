package tech.kayys.wayang.workflow.model;

/**
 * Error action enum.
 */
public enum ErrorAction {
    RETRY,
    AUTO_FIX,
    HUMAN_REVIEW,
    FALLBACK,
    ABORT,
    ESCALATE
}