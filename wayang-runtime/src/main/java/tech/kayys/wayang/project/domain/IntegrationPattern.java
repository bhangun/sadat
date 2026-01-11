package tech.kayys.wayang.project.domain;

import java.time.Instant;
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
import tech.kayys.wayang.project.dto.EndpointConfig;
import tech.kayys.wayang.project.dto.EIPPatternType;
import tech.kayys.wayang.project.dto.ErrorHandlingConfig;
import tech.kayys.wayang.project.dto.TransformationConfig;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Integration Pattern - EIP template
 */
@Entity
@Table(name = "cp_integration_patterns")
public class IntegrationPattern extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pattern_id")
    public UUID patternId;

    @ManyToOne
    @JoinColumn(name = "project_id")
    public WayangProject project;

    @NotNull
    @Column(name = "pattern_name")
    public String patternName;

    @Column(name = "description")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type")
    public EIPPatternType patternType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_config", columnDefinition = "jsonb")
    public EndpointConfig sourceConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_config", columnDefinition = "jsonb")
    public EndpointConfig targetConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transformation", columnDefinition = "jsonb")
    public TransformationConfig transformation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_handling", columnDefinition = "jsonb")
    public ErrorHandlingConfig errorHandling;

    @Column(name = "created_at")
    public Instant createdAt;
}
