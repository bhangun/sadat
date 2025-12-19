package tech.kayys.wayang.agent.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.RestResponse;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
    
    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof AgentNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("AGENT_NOT_FOUND", exception.getMessage()))
                    .build();
        } else if (exception instanceof ValidationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("VALIDATION_ERROR", exception.getMessage()))
                    .build();
        } else if (exception instanceof WorkflowExecutionException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("EXECUTION_ERROR", exception.getMessage()))
                    .build();
        } else if (exception instanceof IntegrationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INTEGRATION_ERROR", exception.getMessage()))
                    .build();
        } else {
            // Log the error for debugging
            System.err.println("Unhandled exception: " + exception.getMessage());
            exception.printStackTrace();
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"))
                    .build();
        }
    }
    
    public static class ErrorResponse {
        public String error;
        public String message;
        public long timestamp;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}