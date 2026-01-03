package tech.kayys.silat.engine;

import java.util.List;

import io.smallrye.mutiny.Uni;
import tech.kayys.silat.model.RunStatus;
import tech.kayys.silat.model.TenantId;
import tech.kayys.silat.model.WorkflowDefinitionId;
import tech.kayys.silat.model.WorkflowRun;
import tech.kayys.silat.model.WorkflowRunId;
import tech.kayys.silat.model.WorkflowRunSnapshot;

interface WorkflowRunRepository {

    Uni<WorkflowRun> persist(WorkflowRun run);

    Uni<WorkflowRun> update(WorkflowRun run);

    Uni<WorkflowRun> findById(WorkflowRunId runId, TenantId tenantId);

    Uni<WorkflowRun> findById(WorkflowRunId runId);

    Uni<WorkflowRunSnapshot> snapshot(WorkflowRunId runId, TenantId tenantId);

    Uni<List<WorkflowRun>> query(
            TenantId tenantId,
            WorkflowDefinitionId definitionId,
            RunStatus status,
            int page,
            int size);

    Uni<Long> countActive(TenantId tenantId);

    <T> Uni<T> withLock(WorkflowRunId runId, LockedOperation<T> op);
}
