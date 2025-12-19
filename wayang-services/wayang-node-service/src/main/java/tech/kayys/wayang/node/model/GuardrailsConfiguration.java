package tech.kayys.wayang.workflow.model;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Guardrails configuration.
 */
@ApplicationScoped
@lombok.Data
class GuardrailsConfiguration {
    private boolean enabled = true;
    private boolean piiDetectionEnabled = true;
    private boolean piiRedactionEnabled = true;
    private boolean blockOnPII = false;
    private boolean promptInjectionDetectionEnabled = true;
    private boolean contentFilteringEnabled = true;
    private boolean qualityValidationEnabled = true;
    private long maxInputSizeBytes = 1024 * 1024; // 1MB
    private long maxOutputSizeBytes = 5 * 1024 * 1024; // 5MB
}