package tech.kayys.wayang.workflow.model;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Audit configuration.
 */
@ApplicationScoped
@lombok.Data
public class AuditConfiguration {
    private boolean enabled = true;
    private boolean hashingEnabled = true;
    private boolean piiRedactionEnabled = true;
    private int retentionDays = 90;
}
