
/**
 * Audit entry model
 */
record AuditEntry(
    String auditId,
    AuditEventType eventType,
    String nodeId,
    String runId,
    String tenantId,
    String userId,
    String correlationId,
    Instant timestamp,
    String status,
    AuditSeverity severity,
    Map<String, Object> metadata
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String auditId;
        private AuditEventType eventType;
        private String nodeId;
        private String runId;
        private String tenantId;
        private String userId;
        private String correlationId;
        private Instant timestamp;
        private String status;
        private AuditSeverity severity = AuditSeverity.INFO;
        private Map<String, Object> metadata = Map.of();
        
        public Builder auditId(String auditId) {
            this.auditId = auditId;
            return this;
        }
        
        public Builder eventType(AuditEventType eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }
        
        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder severity(AuditSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public AuditEntry build() {
            return new AuditEntry(
                auditId, eventType, nodeId, runId, tenantId, 
                userId, correlationId, timestamp, status, 
                severity, metadata
            );
        }
    }
}