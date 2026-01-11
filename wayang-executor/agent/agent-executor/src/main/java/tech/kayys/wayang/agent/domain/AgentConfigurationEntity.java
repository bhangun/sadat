package tech.kayys.wayang.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

/**
 * ============================================================================
 * DATABASE PERSISTENCE LAYER
 * ============================================================================
 * 
 * Production-ready persistence using Hibernate Reactive Panache.
 * 
 * Features:
 * - Multi-tenant data isolation
 * - Optimistic locking
 * - Audit trails
 * - Efficient queries with indexes
 * - Transaction management
 * - Connection pooling
 * 
 * Database Schema:
 * - agent_configurations: Agent settings
 * - conversation_sessions: Session metadata
 * - conversation_messages: Message history
 * - agent_executions: Execution audit log
 * - vector_embeddings: Semantic search (optional)
 */

// ==================== ENTITY CLASSES ====================

/**
 * Agent Configuration Entity
 */
@Entity
@Table(name = "agent_configurations", indexes = {
        @Index(name = "idx_agent_tenant", columnList = "agent_id,tenant_id"),
        @Index(name = "idx_tenant", columnList = "tenant_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "agent_id", "tenant_id" })
})
public class AgentConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "agent_id", nullable = false, length = 255)
    private String agentId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "llm_provider", length = 100)
    private String llmProvider;

    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "memory_enabled")
    private Boolean memoryEnabled;

    @Column(name = "memory_type", length = 50)
    private String memoryType;

    @Column(name = "memory_window_size")
    private Integer memoryWindowSize;

    @Column(name = "enabled_tools", columnDefinition = "TEXT")
    private String enabledTools; // JSON array

    @Column(name = "allow_tool_calls")
    private Boolean allowToolCalls;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "streaming")
    private Boolean streaming;

    @Column(name = "max_iterations")
    private Integer maxIterations;

    @Column(name = "additional_config", columnDefinition = "TEXT")
    private String additionalConfig; // JSON object

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean getMemoryEnabled() {
        return memoryEnabled;
    }

    public void setMemoryEnabled(Boolean memoryEnabled) {
        this.memoryEnabled = memoryEnabled;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public Integer getMemoryWindowSize() {
        return memoryWindowSize;
    }

    public void setMemoryWindowSize(Integer memoryWindowSize) {
        this.memoryWindowSize = memoryWindowSize;
    }

    public String getEnabledTools() {
        return enabledTools;
    }

    public void setEnabledTools(String enabledTools) {
        this.enabledTools = enabledTools;
    }

    public Boolean getAllowToolCalls() {
        return allowToolCalls;
    }

    public void setAllowToolCalls(Boolean allowToolCalls) {
        this.allowToolCalls = allowToolCalls;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Boolean getStreaming() {
        return streaming;
    }

    public void setStreaming(Boolean streaming) {
        this.streaming = streaming;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }

    public String getAdditionalConfig() {
        return additionalConfig;
    }

    public void setAdditionalConfig(String additionalConfig) {
        this.additionalConfig = additionalConfig;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
