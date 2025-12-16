package tech.kayys.wayang.workflow.domain;

import java.time.Instant;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Workflow Execution History Entity
 */
@Entity
@Table(name = "workflow_executions", indexes = {
        @Index(name = "idx_execution_agent", columnList = "agentId"),
        @Index(name = "idx_execution_status", columnList = "status"),
        @Index(name = "idx_execution_time", columnList = "startTime")
})
public class WorkflowExecutionEntity extends PanacheEntityBase {

    @Id
    @Column(length = 36)
    public String executionId;

    @Column(nullable = false, length = 36)
    public String agentId;

    @Column(nullable = false, length = 36)
    public String workflowId;

    @Column(nullable = false, length = 50)
    public String status; // running, completed, failed, cancelled

    @Column(columnDefinition = "TEXT")
    public String inputJson;

    @Column(columnDefinition = "TEXT")
    public String outputJson;

    @Column(columnDefinition = "TEXT")
    public String traceJson;

    @Column(length = 2000)
    public String errorMessage;

    @Column(nullable = false)
    public Instant startTime;

    @Column
    public Instant endTime;

    @Column
    public Long durationMs;

    @Column(length = 255)
    public String triggeredBy;
}