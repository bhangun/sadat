package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentValidationException;
import tech.kayys.wayang.agent.dto.ValidationResult;
import tech.kayys.wayang.agent.dto.CreateAgentRequest;
import tech.kayys.wayang.agent.dto.CreateOrchestratorRequest;

@ApplicationScoped
public class AgentValidationService {

    public Uni<ValidationResult> validateAgentRequest(CreateAgentRequest request) {
        Log.debugf("Validating agent request for: %s", request.name());

        java.util.List<String> errors = new java.util.ArrayList<>();

        if (request.name() == null || request.name().trim().isEmpty()) {
            errors.add("Agent name is required");
        }

        if (request.tenantId() == null || request.tenantId().trim().isEmpty()) {
            errors.add("Tenant ID is required");
        }

        if (request.agentType() == null) {
            errors.add("Agent type is required");
        }

        boolean isValid = errors.isEmpty();
        ValidationResult result = new ValidationResult(isValid, errors);

        if (!isValid) {
            Log.warnf("Agent validation failed: %s", String.join(", ", errors));
        }

        return Uni.createFrom().item(result);
    }

    public Uni<ValidationResult> validateOrchestratorRequest(CreateOrchestratorRequest request) {
        Log.debugf("Validating orchestrator request for: %s", request.name());

        java.util.List<String> errors = new java.util.ArrayList<>();

        if (request.name() == null || request.name().trim().isEmpty()) {
            errors.add("Orchestrator name is required");
        }

        if (request.tenantId() == null || request.tenantId().trim().isEmpty()) {
            errors.add("Tenant ID is required");
        }

        boolean isValid = errors.isEmpty();
        return Uni.createFrom().item(new ValidationResult(isValid, errors));
    }
}