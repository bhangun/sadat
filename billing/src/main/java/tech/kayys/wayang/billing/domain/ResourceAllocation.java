package tech.kayys.wayang.billing.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.billing.model.AllocationStatus;
import tech.kayys.wayang.billing.model.ResourceType;
import tech.kayys.wayang.organization.domain.Organization;

@Entity
@Table(name = "mgmt_resource_allocations", indexes = {
    @Index(name = "idx_res_org", columnList = "organization_id"),
    @Index(name = "idx_res_type", columnList = "resource_type"),
    @Index(name = "idx_res_status", columnList = "status")
})
public class ResourceAllocation extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allocation_id")
    public UUID allocationId;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    public ResourceType resourceType;
    
    @Column(name = "resource_id", length = 255)
    public String resourceId;
    
    @Column(name = "resource_name")
    public String resourceName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public AllocationStatus status = AllocationStatus.PENDING;
    
    @Column(name = "configuration", columnDefinition = "jsonb")
    public Map<String, Object> configuration = new HashMap<>();
    
    @Column(name = "allocated_capacity")
    public long allocatedCapacity;
    
    @Column(name = "used_capacity")
    public long usedCapacity;
    
    @Column(name = "rest_endpoint")
    public String restEndpoint;
    
    @Column(name = "grpc_endpoint")
    public String grpcEndpoint;
    
    @Column(name = "kafka_topic")
    public String kafkaTopic;
    
    @Column(name = "last_health_check")
    public Instant lastHealthCheck;
    
    @Column(name = "health_status")
    public String healthStatus = "UNKNOWN";
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "created_at")
    public Instant createdAt;
    
    @Column(name = "updated_at")
    public Instant updatedAt;
    
    @Column(name = "provisioned_at")
    public Instant provisionedAt;
    
    @Column(name = "deprovisioned_at")
    public Instant deprovisionedAt;
    
    // Business methods
    
    public double getUtilizationPercent() {
        return allocatedCapacity > 0 ? 
            (usedCapacity * 100.0) / allocatedCapacity : 0.0;
    }
    
    public boolean isHealthy() {
        return "HEALTHY".equals(healthStatus);
    }
    
    public void updateUsage(long newUsage) {
        this.usedCapacity = newUsage;
        this.updatedAt = Instant.now();
    }
    
    public boolean isOverCapacity() {
        return usedCapacity > allocatedCapacity;
    }
}
