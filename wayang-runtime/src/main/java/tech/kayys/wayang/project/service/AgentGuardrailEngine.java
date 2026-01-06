package tech.kayys.wayang.project.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.project.domain.AIAgent;
import tech.kayys.wayang.project.dto.Guardrail;
import tech.kayys.wayang.guardrails.dto.GuardrailAction;

/**
 * Guardrail enforcement engine
 */
@ApplicationScoped
public class AgentGuardrailEngine {

    private static final Logger LOG = LoggerFactory.getLogger(AgentGuardrailEngine.class);

    /**
     * Check input against guardrails
     */
    public void checkInput(AIAgent agent, String input) {
        if (agent.guardrails == null) {
            return;
        }

        for (Guardrail guardrail : agent.guardrails) {
            switch (guardrail.type) {
                case PII_DETECTION -> checkPII(input, guardrail);
                case TOXICITY_CHECK -> checkToxicity(input, guardrail);
                case CONTENT_FILTER -> checkContent(input, guardrail);
                default -> LOG.warn("Unknown guardrail type: {}", guardrail.type);
            }
        }
    }

    /**
     * Check output against guardrails
     */
    public String checkOutput(AIAgent agent, String output) {
        if (agent.guardrails == null) {
            return output;
        }

        String processed = output;

        for (Guardrail guardrail : agent.guardrails) {
            processed = switch (guardrail.type) {
                case PII_DETECTION -> sanitizePII(processed, guardrail);
                case CONTENT_FILTER -> filterContent(processed, guardrail);
                default -> processed;
            };
        }

        return processed;
    }

    private void checkPII(String text, Guardrail guardrail) {
        // Simplified PII detection
        if (text.matches(".*\\d{3}-\\d{2}-\\d{4}.*")) { // SSN pattern
            if (guardrail.action == GuardrailAction.BLOCK) {
                throw new SecurityException("PII detected in input");
            }
        }
    }

    private void checkToxicity(String text, Guardrail guardrail) {
        // Simplified toxicity check
        List<String> toxicWords = List.of("offensive", "toxic");
        for (String word : toxicWords) {
            if (text.toLowerCase().contains(word)) {
                if (guardrail.action == GuardrailAction.BLOCK) {
                    throw new SecurityException("Toxic content detected");
                }
            }
        }
    }

    private void checkContent(String text, Guardrail guardrail) {
        // Content filtering logic
    }

    private String sanitizePII(String text, Guardrail guardrail) {
        // Sanitize PII in output
        return text.replaceAll("\\d{3}-\\d{2}-\\d{4}", "XXX-XX-XXXX");
    }

    private String filterContent(String text, Guardrail guardrail) {
        // Filter content
        return text;
    }
}
