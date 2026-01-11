package tech.kayys.wayang.project.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import tech.kayys.wayang.project.dto.ProjectType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;

// ==================== DOMAIN MODEL ====================

/**
 * Control Plane Project - Container for workflows, agents, and integrations
 */
@Entity
@Table(name = "wayang_project")
public class WayangProject extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "project_id")
    public UUID projectId;

    @NotNull
    @Column(name = "tenant_id")
    public String tenantId;

    @NotNull
    @Column(name = "project_name")
    public String projectName;

    @Column(name = "description")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type")
    public ProjectType projectType;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "is_active")
    public boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    public List<WorkflowTemplate> workflows = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    public List<AIAgent> agents = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    public List<IntegrationPattern> integrations = new ArrayList<>();
}