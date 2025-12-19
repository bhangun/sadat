package tech.kayys.wayang.node.exception;

/**
 * Custom exceptions.
 */
class NodeNotFoundException extends RuntimeException {
    public NodeNotFoundException(String message) {
        super(message);
    }
}
