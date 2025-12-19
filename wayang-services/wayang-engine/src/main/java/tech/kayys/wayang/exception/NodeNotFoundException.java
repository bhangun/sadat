package tech.kayys.wayang.exception;

import jakarta.ws.rs.core.Response;
import java.util.Map;

public class NodeNotFoundException extends DesignerException {
    public NodeNotFoundException(String message) {
        super("NODE_NOT_FOUND", message);
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.NOT_FOUND;
    }
}
