package tech.kayys.wayang.workflow.exception;

/**
 * Concurrent modification exception (optimistic locking)
 */
class ConcurrentModificationException extends WayangException {
    public ConcurrentModificationException(String message) {
        super("CONCURRENT_MODIFICATION", message);
    }

    public ConcurrentModificationException(String message, Throwable cause) {
        super("CONCURRENT_MODIFICATION", message, cause);
    }
}
