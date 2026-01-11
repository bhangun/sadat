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
import tech.kayys.wayang.project.dto.AgentCapability;
import tech.kayys.wayang.project.dto.AgentStatus;
import tech.kayys.wayang.project.dto.AgentTool;
import tech.kayys.wayang.project.dto.AgentType;
import tech.kayys.wayang.project.dto.Guardrail;
import tech.kayys.wayang.project.dto.LLMConfig;
import tech.kayys.wayang.project.dto.MemoryConfig;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * AI Agent - Autonomous agent definition
 */
@Entity
@Table(name = "cp_ai_agents")
public class AIAgent extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "agent_id")
    public UUID agentId;

    @ManyToOne
    @JoinColumn(name = "project_id")
    public WayangProject project;

    @NotNull
    @Column(name = "agent_name")
    public String agentName;

    @Column(name = "description")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type")
    public AgentType agentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "llm_config", columnDefinition = "jsonb")
    public LLMConfig llmConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    public List<AgentCapability> capabilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tools", columnDefinition = "jsonb")
    public List<AgentTool> tools;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "memory_config", columnDefinition = "jsonb")
    public MemoryConfig memoryConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "guardrails", columnDefinition = "jsonb")
    public List<Guardrail> guardrails;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public AgentStatus status = AgentStatus.INACTIVE;

    @Column(name = "created_at")
    public Instant createdAt;
}
