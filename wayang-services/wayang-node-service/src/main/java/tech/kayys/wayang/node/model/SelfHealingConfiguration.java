package tech.kayys.wayang.workflow.model;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Self-healing configuration.
 */
@ApplicationScoped
@lombok.Data
class SelfHealingConfiguration {
    private boolean enabled = true;
    private int maxHealingAttempts = 2;
    private boolean useLLMHealing = true;
    private String healingModel = "gpt-4";
}
