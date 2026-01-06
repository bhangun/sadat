package tech.kayys.wayang.agent.orchestrator.exception;

public class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
        super(message);
    }
}