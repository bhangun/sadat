package tech.kayys.wayang.workflow.kernel;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import tech.kayys.wayang.workflow.v1.WorkflowEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for workflow event persistence
 */
@ApplicationScoped
public class EventRepository implements PanacheRepository<WorkflowEvent> {

    @jakarta.inject.Inject
    EntityManager entityManager;

    @Inject
    EventSerializer eventSerializer;

    public Uni<WorkflowEvent> save(WorkflowEvent event) {
        return Uni.createFrom().deferred(() -> {
            // Generate ID if not present
            if (event.id() == null) {
                WorkflowEvent withId = WorkflowEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .runId(event.runId())
                        .type(event.type())
                        .timestamp(event.timestamp())
                        .data(event.data())
                        .build();
                return persist(withId);
            }
            return persist(event);
        });
    }

    public Uni<List<WorkflowEvent>> findByRunId(String runId) {
        return find("runId", runId)
                .list();
    }

    public Uni<List<WorkflowEvent>> findByRunIdAndType(String runId, String type) {
        return find("runId = ?1 and type = ?2", runId, type)
                .list();
    }

    public Uni<List<WorkflowEvent>> findByRunIdAfterTimestamp(String runId, Instant after) {
        return find("runId = ?1 and timestamp > ?2 order by timestamp asc", runId, after)
                .list();
    }

    public Uni<List<WorkflowEvent>> findByRunIdBeforeTimestamp(String runId, Instant before) {
        return find("runId = ?1 and timestamp < ?2 order by timestamp desc", runId, before)
                .list();
    }

    public Uni<WorkflowEvent> findLatestByRunId(String runId) {
        return find("runId = ?1 order by timestamp desc", runId)
                .firstResult();
    }

    public Uni<WorkflowEvent> findLatestByRunIdAndType(String runId, String type) {
        return find("runId = ?1 and type = ?2 order by timestamp desc", runId, type)
                .firstResult();
    }

    public Uni<Long> countByRunId(String runId) {
        return count("runId", runId);
    }

    public Uni<Long> deleteByRunId(String runId) {
        return delete("runId", runId);
    }

    public Uni<Void> deleteOldEvents(Instant cutoff) {
        return delete("timestamp < ?1", cutoff)
                .replaceWithVoid();
    }

    @Transactional
    public Uni<Void> replaceEvents(String runId, List<WorkflowEvent> newEvents) {
        return deleteByRunId(runId)
                .flatMap(deletedCount -> {
                    LOG.debug("Deleted {} events for run {}", deletedCount, runId);

                    // Save new events
                    List<Uni<WorkflowEvent>> saveUnis = newEvents.stream()
                            .map(this::save)
                            .toList();

                    return Uni.combine().all().unis(saveUnis).asList()
                            .onItem().invoke(saved -> LOG.debug("Saved {} new events for run {}", saved.size(), runId))
                            .replaceWithVoid();
                });
    }

    public Uni<List<WorkflowEvent>> getEventsPaginated(String runId, int page, int size) {
        return find("runId = ?1 order by timestamp asc", runId)
                .page(page, size)
                .list();
    }

    public Uni<WorkflowEvent> findByIdWithLock(String id) {
        return findById(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public Uni<List<WorkflowEvent>> findEventsWithDataField(String runId, String fieldName, Object value) {
        // This is a simplified version. In reality, you'd need to query JSON fields
        // which depends on your database (PostgreSQL jsonb, MySQL JSON, etc.)
        return find("runId = ?1", runId)
                .list()
                .map(events -> events.stream()
                        .filter(event -> event.data() != null &&
                                value.equals(event.data().get(fieldName)))
                        .toList());
    }

    public Uni<Map<String, Long>> getEventTypeDistribution(String runId) {
        return find("runId = ?1", runId)
                .list()
                .map(events -> {
                    Map<String, Long> distribution = new java.util.HashMap<>();
                    events.forEach(event -> distribution.merge(event.type(), 1L, Long::sum));
                    return distribution;
                });
    }

    public Uni<Instant> getFirstEventTimestamp(String runId) {
        return find("runId = ?1 order by timestamp asc", runId)
                .firstResult()
                .map(event -> event != null ? event.timestamp() : null);
    }

    public Uni<Instant> getLastEventTimestamp(String runId) {
        return find("runId = ?1 order by timestamp desc", runId)
                .firstResult()
                .map(event -> event != null ? event.timestamp() : null);
    }

    public Uni<List<WorkflowEvent>> findEventsByTimeRange(Instant from, Instant to) {
        return find("timestamp >= ?1 and timestamp <= ?2 order by timestamp asc", from, to)
                .list();
    }

    public Uni<String> exportEventsAsJson(String runId) {
        return findByRunId(runId)
                .flatMap(events -> {
                    EventExport export = new EventExport();
                    export.setRunId(runId);
                    export.setExportTime(Instant.now());
                    export.setEventCount(events.size());
                    export.setEvents(events);

                    return eventSerializer.serialize(export)
                            .onFailure().recoverWithItem(th -> {
                                LOG.error("Error exporting events as JSON", th);
                                return "{\"error\": \"" + th.getMessage() + "\"}";
                            });
                });
    }

    public static class EventExport {
        private String runId;
        private Instant exportTime;
        private int eventCount;
        private List<WorkflowEvent> events;

        // Getters and setters
        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public Instant getExportTime() {
            return exportTime;
        }

        public void setExportTime(Instant exportTime) {
            this.exportTime = exportTime;
        }

        public int getEventCount() {
            return eventCount;
        }

        public void setEventCount(int eventCount) {
            this.eventCount = eventCount;
        }

        public List<WorkflowEvent> getEvents() {
            return events;
        }

        public void setEvents(List<WorkflowEvent> events) {
            this.events = events;
        }
    }
}