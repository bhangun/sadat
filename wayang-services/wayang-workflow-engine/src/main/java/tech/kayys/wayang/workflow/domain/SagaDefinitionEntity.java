package tech.kayys.wayang.workflow.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tech.kayys.wayang.workflow.model.saga.CompensationActionDefinition;
import tech.kayys.wayang.workflow.model.saga.CompensationStrategy;

/**
 * SagaDefinitionEntity - Represents a saga definition in the workflow engine
 * Key considerations:
 * 1. JSONB storage for flexible compensation action definitions
 * 2. Support for saga orchestration patterns
 * 3. Optimistic locking for concurrent updates
 */
@Entity
@Table(name = "saga_definitions", indexes = {
        @Index(name = "idx_saga_def_workflow", columnList = "workflow_id"),
        @Index(name = "idx_saga_def_tenant", columnList = "tenant_id"),
        @Index(name = "idx_saga_def_active", columnList = "active"),
        @Index(name = "idx_saga_def_comp_strategy", columnList = "compensation_strategy")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SagaDefinitionEntity extends PanacheEntityBase {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotBlank(message = "Workflow ID cannot be blank")
    @Size(max = 255, message = "Workflow ID cannot exceed 255 characters")
    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "version")
    @Version
    private Long version = 1L;

    @NotBlank(message = "Tenant ID cannot be blank")
    @Size(max = 100, message = "Tenant ID cannot exceed 100 characters")
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotBlank(message = "Pivot node cannot be blank")
    @Size(max = 255, message = "Pivot node cannot exceed 255 characters")
    @Column(name = "pivot_node", nullable = false)
    private String pivotNode;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "compensations", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, CompensationActionDefinition> compensations = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_retriable_nodes", joinColumns = @JoinColumn(name = "saga_definition_id"), indexes = @Index(name = "idx_retriable_nodes", columnList = "saga_definition_id"))
    @Column(name = "node_id", nullable = false, length = 100)
    @BatchSize(size = 20)
    private Set<String> retriableNodes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_parameters", joinColumns = @JoinColumn(name = "saga_definition_id"), indexes = @Index(name = "idx_saga_params", columnList = "saga_definition_id"))
    @MapKeyColumn(name = "param_key", length = 100)
    @Column(name = "param_value", length = 500)
    @BatchSize(size = 20)
    private Map<String, String> parameters = new HashMap<>();

    @Column(name = "max_retries")
    @Min(value = 0, message = "Max retries cannot be negative")
    @Max(value = 100, message = "Max retries cannot exceed 100")
    private Integer maxRetries = 3;

    @Column(name = "retry_delay_ms")
    @Min(value = 0, message = "Retry delay cannot be negative")
    @Max(value = 300000, message = "Retry delay cannot exceed 5 minutes (300000ms)")
    private Long retryDelayMs = 1000L;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_strategy", length = 50)
    private CompensationStrategy compensationStrategy = CompensationStrategy.BACKWARD;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Default constructor for JPA
    public SagaDefinitionEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor for creating new entities
    public SagaDefinitionEntity(String workflowId, String name, String tenantId, String pivotNode) {
        this();
        this.workflowId = workflowId;
        this.name = name;
        this.tenantId = tenantId;
        this.pivotNode = pivotNode;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public boolean hasCompensationForNode(String nodeId) {
        return compensations != null && compensations.containsKey(nodeId);
    }

    public boolean isNodeRetriable(String nodeId) {
        return retriableNodes != null && retriableNodes.contains(nodeId);
    }

    public void addRetriableNode(String nodeId) {
        if (retriableNodes == null) {
            retriableNodes = new HashSet<>();
        }
        retriableNodes.add(nodeId);
    }

    public void addCompensation(String nodeId, CompensationActionDefinition compensation) {
        if (compensations == null) {
            compensations = new HashMap<>();
        }
        compensations.put(nodeId, compensation);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPivotNode() {
        return pivotNode;
    }

    public void setPivotNode(String pivotNode) {
        this.pivotNode = pivotNode;
    }

    public Map<String, CompensationActionDefinition> getCompensations() {
        return compensations != null ? new HashMap<>(compensations) : new HashMap<>();
    }

    public void setCompensations(Map<String, CompensationActionDefinition> compensations) {
        this.compensations = compensations != null ? new HashMap<>(compensations) : new HashMap<>();
    }

    public Set<String> getRetriableNodes() {
        return retriableNodes != null ? new HashSet<>(retriableNodes) : new HashSet<>();
    }

    public void setRetriableNodes(Set<String> retriableNodes) {
        this.retriableNodes = retriableNodes != null ? new HashSet<>(retriableNodes) : new HashSet<>();
    }

    public Map<String, String> getParameters() {
        return parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(Long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public CompensationStrategy getCompensationStrategy() {
        return compensationStrategy;
    }

    public void setCompensationStrategy(CompensationStrategy compensationStrategy) {
        this.compensationStrategy = compensationStrategy;
    }

    public Map<String, Object> getMetadata() {
        return metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SagaDefinitionEntity that = (SagaDefinitionEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SagaDefinitionEntity{" +
                "id='" + id + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", name='" + name + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", active=" + active +
                ", version=" + version +
                '}';
    }
}
