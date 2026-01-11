package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Audit & Compliance
 */
interface WorkflowComplianceService {

    /**
     * Generate compliance report
     */
    Uni<ComplianceReport> generateReport(
        tech.kayys.silat.core.domain.TenantId tenantId,
        ComplianceStandard standard, // SOC2, HIPAA, GDPR, etc.
        java.time.Instant startDate,
        java.time.Instant endDate
    );

    /**
     * Check workflow compliance
     */
    Uni<ComplianceResult> checkCompliance(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        List<ComplianceRule> rules
    );

    /**
     * Data retention enforcement
     */
    Uni<Void> enforceRetention(
        RetentionPolicy policy
    );
}