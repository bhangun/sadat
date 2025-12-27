package tech.kayys.wayang.workflow.scheduler.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "workflow_schedules", indexes = {
        @Index(name = "idx_next_execution", columnList = "next_execution_at"),
        @Index(name = "idx_tenant_workflow", columnList = "tenant_id, workflow_id")
})
public class WorkflowScheduleEntity {
    @Id
    @Column(name = "schedule_id")
    private String scheduleId;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "next_execution_at")
    private Instant nextExecutionAt;

    @Column(name = "enabled")
    private boolean enabled;

    @Version
    private Long version; // Optimistic locking

}
