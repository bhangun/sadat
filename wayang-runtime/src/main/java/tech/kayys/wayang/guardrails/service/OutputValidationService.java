package tech.kayys.wayang.guardrails.service;

import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.dto.GuardrailAction;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailSeverity;
import tech.kayys.wayang.guardrails.dto.ValidationRule;

/**
 * Output validation service
 */
@ApplicationScoped
public class OutputValidationService {

    public Uni<GuardrailCheckResult> validate(
            String output,
            List<ValidationRule> rules) {

        return Uni.createFrom().deferred(() -> {
            for (ValidationRule rule : rules) {
                if (!rule.validate(output)) {
                    return Uni.createFrom().item(GuardrailCheckResult.violation(
                            "output_validation",
                            GuardrailSeverity.LOW,
                            GuardrailAction.WARN,
                            "Output validation failed: " + rule.description(),
                            Map.of("rule", rule.name())));
                }
            }

            return Uni.createFrom().item(GuardrailCheckResult.passed("output_validation"));
        });
    }
}