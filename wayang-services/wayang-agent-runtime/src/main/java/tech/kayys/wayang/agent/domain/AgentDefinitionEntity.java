package tech.kayys.wayang.agent.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Agent Definition Entity - Persistent storage
 */
@Entity
@Table(name = "agent_definitions", indexes = {
        @Index(name = "idx_agent_name", columnList = "name"),
        @Index(name = "idx_agent_status", columnList = "status"),
        @Index(name = "idx_agent_type", columnList = "type")
})
public class AgentDefinitionEntity extends PanacheEntityBase {

    @Id
    @Column(length = 36)
    public String id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(length = 1000)
    public String description;

    @Column(nullable = false, length = 50)
    public String type;

    @Column(nullable = false, length = 50)
    public String status;

    @Column(columnDefinition = "TEXT")
    public String definitionJson; // Full JSON definition

    @Column(nullable = false)
    public Instant createdAt;

    @Column(nullable = false)
    public Instant updatedAt;

    @Column(length = 255)
    public String createdBy;

    @Column(length = 50)
    public String version;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}