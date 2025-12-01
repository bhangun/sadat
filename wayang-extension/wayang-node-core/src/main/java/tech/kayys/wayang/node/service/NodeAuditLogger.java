package tech.kayys.wayang.node.core.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles audit logging for node operations.
 * 
 * Captures all node lifecycle events, executions, and errors
 * for compliance and debugging purposes.
 */
@ApplicationScoped
public class NodeAuditLogger {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeAuditLogger.class);
    
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Map<String, AuditEntry> auditCache;
    
    @Inject
    public NodeAuditLogger(
        ObjectMapper objectMapper,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.auditCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Log node creation event
     */
    public void logNodeCreated(
        String nodeId,
        String tenantId,
        String userId,
        Map<String, Object> metadata
    ) {
        AuditEntry entry = AuditEntry.builder()
            .auditId(UUID.randomUUID().toString())
            .eventType(AuditEventType.NODE_CREATED)
            .nodeId(nodeId)
            .tenantId(tenantId)
            .userId(userId)
            .timestamp(Instant.now())
            .metadata(metadata)
            .build();
        
        persistAuditEntry(entry);
        meterRegistry.counter("audit.event", "type", "NODE_CREATED").increment();
    }
    
    /**
     * Log node execution start
     */
    public String logExecutionStarted(
        String nodeId,
        String runId,
        String tenantId,
        Map<String, Object> inputs
    ) {
        String executionId = UUID.randomUUID().toString();
        
        AuditEntry entry = AuditEntry.builder()
            .auditId(executionId)
            .eventType(AuditEventType.EXECUTION_STARTED)
            .nodeId(nodeId)
            .runId(runId)
            .tenantId(tenantId)
            .timestamp(Instant.now())
            .metadata(Map.of("inputs", redactSensitiveData(inputs)))
            .build();
        
        auditCache.put(executionId, entry);
        persistAuditEntry(entry);
        
        meterRegistry.counter("audit.event", "type", "EXECUTION_STARTED").increment();
        
        return executionId;
    }
    
    /**
     * Log node execution completion
     */
    public void logExecutionCompleted(
        String executionId,
        String status,
        Map<String, Object> outputs,
        long durationMs
    ) {
        AuditEntry entry = AuditEntry.builder()
            .auditId(UUID.randomUUID().toString())
            .eventType(AuditEventType.EXECUTION_COMPLETED)
            .correlationId(executionId)
            .timestamp(Instant.now())
            .status(status)
            .metadata(Map.of(
                "outputs", redactSensitiveData(outputs),
                "durationMs", durationMs
            ))
            .build();
        
        persistAuditEntry(entry);
        auditCache.remove(executionId);
        
        meterRegistry.counter("audit.event", "type", "EXECUTION_COMPLETED").increment();
        meterRegistry.timer("audit.execution.duration").record(
            java.time.Duration.ofMillis(durationMs)
        );
    }
    
    /**
     * Log node execution failure
     */
    public void logExecutionFailed(
        String executionId,
        String errorCode,
        String errorMessage,
        boolean retryable
    ) {
        AuditEntry entry = AuditEntry.builder()
            .auditId(UUID.randomUUID().toString())
            .eventType(AuditEventType.EXECUTION_FAILED)
            .correlationId(executionId)
            .timestamp(Instant.now())
            .status("FAILED")
            .metadata(Map.of(
                "errorCode", errorCode,
                "errorMessage", errorMessage,
                "retryable", retryable
            ))
            .build();
        
        persistAuditEntry(entry);
        auditCache.remove(executionId);
        
        meterRegistry.counter("audit.event", 
            "type", "EXECUTION_FAILED",
            "retryable", String.valueOf(retryable)
        ).increment();
    }
    
    /**
     * Log validation failure
     */
    public void logValidationFailed(
        String nodeId,
        String tenantId,
        String validationType,
        java.util.List<String> errors
    ) {
        AuditEntry entry = AuditEntry.builder()
            .auditId(UUID.randomUUID().toString())
            .eventType(AuditEventType.VALIDATION_FAILED)
            .nodeId(nodeId)
            .tenantId(tenantId)
            .timestamp(Instant.now())
            .metadata(Map.of(
                "validationType", validationType,
                "errors", errors
            ))
            .build();
        
        persistAuditEntry(entry);
        
        meterRegistry.counter("audit.event", "type", "VALIDATION_FAILED").increment();
    }
    
    /**
     * Log quota exceeded
     */
    public void logQuotaExceeded(
        String nodeId,
        String tenantId,
        String quotaType,
        Object currentValue,
        Object limitValue
    ) {
        AuditEntry entry = AuditEntry.builder()
            .auditId(UUID.randomUUID().toString())
            .eventType(AuditEventType.QUOTA_EXCEEDED)
            .nodeId(nodeId)
            .tenantId(tenantId)
            .timestamp(Instant.now())
            .metadata(Map.of(
                "quotaType", quotaType,
                "currentValue", currentValue,
                "limitValue", limitValue
            ))
            .build();
        
        persistAuditEntry(entry);
        
        meterRegistry.counter("audit.event", 
            "type", "QUOTA_EXCEEDED",
            "quota_type", quotaType
        ).increment();
    }
    
    /**
     * Log security violation
     */
    public void logSecurityViolation(
        String nodeId,
        String tenantId,
        String violationType,
        String description
    ) {
        AuditEntry entry = AuditEntry.builder()
            .auditId(UUID.randomUUID().toString())
            .eventType(AuditEventType.SECURITY_VIOLATION)
            .nodeId(nodeId)
            .tenantId(tenantId)
            .timestamp(Instant.now())
            .severity(AuditSeverity.HIGH)
            .metadata(Map.of(
                "violationType", violationType,
                "description", description
            ))
            .build();
        
        persistAuditEntry(entry);
        
        meterRegistry.counter("audit.event", 
            "type", "SECURITY_VIOLATION",
            "violation_type", violationType
        ).increment();
        
        LOG.warn("Security violation detected: {} - {} - {}", 
            nodeId, violationType, description);
    }
    
    /**
     * Redact sensitive data from logs
     */
    private Map<String, Object> redactSensitiveData(Map<String, Object> data) {
        if (data == null) {
            return Map.of();
        }
        
        Map<String, Object> redacted = new ConcurrentHashMap<>(data);
        
        // List of keys to redact
        String[] sensitiveKeys = {
            "password", "secret", "token", "key", "credential",
            "apiKey", "privateKey", "accessToken"
        };
        
        for (String key : sensitiveKeys) {
            if (redacted.containsKey(key)) {
                redacted.put(key, "***REDACTED***");
            }
        }
        
        return Map.copyOf(redacted);
    }
    
    /**
     * Persist audit entry
     * In production, this would write to a database or audit service
     */
    private void persistAuditEntry(AuditEntry entry) {
        try {
            // Convert to JSON for structured logging
            String json = objectMapper.writeValueAsString(entry);
            
            // Log with appropriate level based on event type
            switch (entry.eventType()) {
                case EXECUTION_FAILED, VALIDATION_FAILED, QUOTA_EXCEEDED, SECURITY_VIOLATION ->
                    LOG.error("AUDIT: {}", json);
                default ->
                    LOG.info("AUDIT: {}", json);
            }
            
            // In production: persist to audit database/service
            // auditService.persist(entry);
            
        } catch (Exception e) {
            LOG.error("Failed to persist audit entry", e);
        }
    }
    
    /**
     * Get audit statistics
     */
    public AuditStatistics getStatistics() {
        return new AuditStatistics(
            auditCache.size(),
            meterRegistry.counter("audit.event").count()
        );
    }
}
