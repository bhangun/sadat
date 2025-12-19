package tech.kayys.wayang.agent.dto;

public class AgentValidationException extends RuntimeException {
    public AgentValidationException(java.util.List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
    }
}