package tech.kayys.wayang.project.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import tech.kayys.wayang.domain.CanvasDefinition;
import tech.kayys.wayang.project.dto.TemplateType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Workflow Template - Visual workflow definition
 */
@Entity
@Table(name = "cp_workflow_templates")
public class WorkflowTemplate extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id")
    public UUID templateId;

    @ManyToOne
    @JoinColumn(name = "project_id")
    public WayangProject project;

    @NotNull
    @Column(name = "template_name")
    public String templateName;

    @Column(name = "description")
    public String description;

    @Column(name = "version")
    public String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type")
    public TemplateType templateType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canvas_definition", columnDefinition = "jsonb")
    public CanvasDefinition canvasDefinition;

    @Column(name = "workflow_definition_id")
    public String workflowDefinitionId;

    @Column(name = "is_published")
    public boolean isPublished = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    public List<String> tags;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
