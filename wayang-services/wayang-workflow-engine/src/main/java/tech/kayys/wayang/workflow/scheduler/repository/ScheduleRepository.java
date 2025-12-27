package tech.kayys.wayang.workflow.scheduler.repository;

import java.time.Instant;
import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.scheduler.domain.WorkflowScheduleEntity;

@ApplicationScoped
public class ScheduleRepository implements PanacheRepositoryBase<WorkflowScheduleEntity, String> {

    public Uni<List<WorkflowScheduleEntity>> findDueSchedules(Instant now) {
        return find("enabled = true and nextExecutionAt <= ?1", now).list();
    }
}