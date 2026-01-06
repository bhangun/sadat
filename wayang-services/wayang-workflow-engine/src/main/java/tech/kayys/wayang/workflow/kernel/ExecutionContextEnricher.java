package tech.kayys.wayang.workflow.kernel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.kernel.ExecutionContext;
import tech.kayys.wayang.workflow.security.context.SecurityContextHolder;
import tech.kayys.wayang.workflow.service.TelemetryService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.engine.config.spi.ConfigurationService;

/**
 * Enriches execution context with additional metadata
 */
@ApplicationScoped
public class ExecutionContextEnricher {

    @Inject
    TelemetryService telemetryService;

    @Inject
    ConfigurationService configService;

    public ExecutionContext enrich(ExecutionContext context) {
        if (context == null) {
            return createNewContext();
        }

        ExecutionContext.Builder builder = ExecutionContext.builder()
                .variables(context.getVariables() != null ? new HashMap<>(context.getVariables()) : new HashMap<>())
                .metadata(context.getMetadata() != null ? new HashMap<>(context.getMetadata()) : new HashMap<>())
                .tenantId(context.getTenantId())
                .workflowRunId(context.getWorkflowRunId())
                .nodeId(context.getNodeId());

        // Add enrichment
        enrichWithExecutionId(builder);
        enrichWithTimestamps(builder);
        enrichWithSecurityContext(builder);
        enrichWithTelemetry(builder);
        enrichWithConfiguration(builder);

        return builder.build();
    }

    private ExecutionContext createNewContext() {
        return ExecutionContext.builder()
                .variables(new HashMap<>())
                .metadata(new HashMap<>())
                .tenantId(SecurityContextHolder.getCurrentTenantId())
                .executionId(generateExecutionId())
                .build();
    }

    private void enrichWithExecutionId(ExecutionContext.Builder builder) {
        if (builder.build().getExecutionId() == null) {
            builder.executionId(generateExecutionId());
        }
    }

    private void enrichWithTimestamps(ExecutionContext.Builder builder) {
        Map<String, Object> metadata = builder.build().getMetadata();
        metadata.put("enrichedAt", Instant.now().toString());
        metadata.put("enricherVersion", "1.0.0");
        builder.metadata(metadata);
    }

    private void enrichWithSecurityContext(ExecutionContext.Builder builder) {
        if (SecurityContextHolder.hasContext()) {
            Map<String, Object> metadata = builder.build().getMetadata();
            metadata.put("userId", SecurityContextHolder.getCurrentUserId());
            metadata.put("userRoles", SecurityContextHolder.getCurrentUserRoles());
            metadata.put("authMethod", SecurityContextHolder.getAuthenticationMethod());
            builder.metadata(metadata);
        }
    }

    private void enrichWithTelemetry(ExecutionContext.Builder builder) {
        ExecutionContext context = builder.build();

        // Add trace and span IDs
        String traceId = telemetryService.generateTraceId();
        String spanId = telemetryService.generateSpanId();

        Map<String, Object> metadata = context.getMetadata();
        metadata.put("traceId", traceId);
        metadata.put("spanId", spanId);
        metadata.put("telemetryEnabled", telemetryService.isEnabled());

        builder.metadata(metadata);
    }

    private void enrichWithConfiguration(ExecutionContext.Builder builder) {
        ExecutionContext context = builder.build();
        String tenantId = context.getTenantId();

        if (tenantId != null) {
            Map<String, Object> config = configService.getTenantConfig(tenantId);
            if (config != null && !config.isEmpty()) {
                Map<String, Object> metadata = context.getMetadata();
                metadata.put("tenantConfig", config);
                builder.metadata(metadata);
            }
        }
    }

    private String generateExecutionId() {
        return "exec-" + UUID.randomUUID().toString().substring(0, 8) +
                "-" + System.currentTimeMillis();
    }
}
