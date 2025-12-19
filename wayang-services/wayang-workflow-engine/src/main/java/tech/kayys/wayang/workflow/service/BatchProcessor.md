package tech.kayys.wayang.workflow.service;

import java.util.List;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.api.model.WorkflowEvent;

/**
 * Batch Processing Optimization
 */
@ApplicationScoped
public class BatchProcessor {

    /**
     * Bulk insert with COPY command (PostgreSQL)
     */
    public Uni<Integer> bulkInsertEvents(List<WorkflowEvent> events) {
        if (events.size() < 100) {
            // Use regular insert for small batches
            return regularInsert(events);
        }

        // Use COPY for large batches (10x faster)
        return pgCopyClient.copy(
                "workflow_events",
                events,
                List.of("id", "run_id", "sequence", "type", "data", "created_at"));
    }

    /**
     * Parallel batch processing
     */
    public <T> Uni<List<T>> processBatch(
            List<T> items,
            Function<T, Uni<T>> processor,
            int parallelism) {
        return Multi.createFrom().iterable(items)
                .onItem().transformToUniAndMerge(processor)
                .merge(parallelism)
                .collect().asList();
    }
}
