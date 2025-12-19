package tech.kayys.wayang.workflow.exception;

/**
 * Invalid state transition exception
 */
class InvalidStateTransitionException extends WayangException {
    public InvalidStateTransitionException(String message) {
        super("INVALID_STATE_TRANSITION", message);
    }
}
