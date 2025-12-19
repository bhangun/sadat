package tech.kayys.wayang.exception;

import java.util.Map;
import jakarta.ws.rs.core.Response;

public class NodeLockedException extends DesignerException {
    public NodeLockedException(String message) {
        super("NODE_LOCKED", message);
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.CONFLICT;
    }
}
