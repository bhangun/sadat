package tech.kayys.wayang.agent.exception;

public class IntegrationException extends RuntimeException {
    public IntegrationException(String message) {
        super("Integration error: " + message);
    }
    
    public IntegrationException(String message, Throwable cause) {
        super("Integration error: " + message, cause);
    }
}