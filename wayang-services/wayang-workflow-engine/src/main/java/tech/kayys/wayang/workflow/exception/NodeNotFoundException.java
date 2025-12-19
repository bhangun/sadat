package tech.kayys.wayang.workflow.exception;

/**
 * Run not found exception
 */
public class NodeNotFoundException extends WayangException {
    public NodeNotFoundException(String message) {
        super("NODE_NOT_FOUND", message);
    }
}
