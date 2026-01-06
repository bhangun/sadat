package tech.kayys.wayang.agent.orchestrator.exception;

import java.util.List;

import tech.kayys.wayang.agent.dto.ExecutionError;

/**
 * Orchestration Exception
 */
class OrchestrationException extends RuntimeException {
    private final List<ExecutionError> errors;
    
    public OrchestrationException(String message, List<ExecutionError> errors) {
        super(message);
        this.errors = errors;
    }
    
    public List<ExecutionError> getErrors() {
        return errors;
    }
}
