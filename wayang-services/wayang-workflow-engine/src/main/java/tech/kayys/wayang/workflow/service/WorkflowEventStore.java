package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import tech.kayys.wayang.workflow.domain.WorkflowEventEntity;
import tech.kayys.wayang.workflow.api.model.WorkflowEvent;

import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event Store for Workflow Runs
 * 
 * Design:
 * - Append-only log (immutable events)
 * - Sequential event ordering via sequence numbers
 * - JSONB storage for event data (PostgreSQL)
 * - Supports event replay and time-travel queries
 * - Stream processing for real-time event consumption
 */
@ApplicationScoped
public class WorkflowEventStore implements PanacheRepositoryBase<WorkflowEventEntity, String> {

        private static final Logger log = LoggerFactory.getLogger(WorkflowEventStore.class);

        /**
         * Append event to stream (atomic operation)
         */
        public Uni<Long> append(String runId, WorkflowEvent event) {
                log.debug("Appending event {} to run {}", event.type(), runId);

                return getNextSequence(runId)
                                .onItem().transformToUni(sequence -> {
                                        WorkflowEventEntity entity = new WorkflowEventEntity(
                                                        UUID.randomUUID().toString(),
                                                        runId,
                                                        sequence,
                                                        event.type(),
                                                        event.data(),
                                                        Instant.now());

                                        return persist(entity)
                                                        .map(e -> e.sequence);
                                });
        }

        /**
         * Get all events for a run (ordered by sequence)
         */
        public Uni<List<WorkflowEvent>> getEvents(String runId) {
                return find("runId = ?1", Sort.by("sequence"), runId)
                                .list()
                                .map(entities -> entities.stream()
                                                .map(this::toEvent)
                                                .toList());
        }

        /**
         * Get events after specific sequence number
         */
        public Uni<List<WorkflowEvent>> getEventsAfter(String runId, long afterSequence) {
                return find("runId = ?1 and sequence > ?2", Sort.by("sequence"), runId, afterSequence)
                                .list()
                                .map(entities -> entities.stream()
                                                .map(this::toEvent)
                                                .toList());
        }

        /**
         * Stream events in real-time
         */
        public Multi<WorkflowEvent> streamEvents(String runId) {
                return stream("runId = ?1", Sort.by("sequence"), runId)
                                .map(this::toEvent);
        }

        /**
         * Get event count for a run
         */
        public Uni<Long> getEventCount(String runId) {
                return count("runId", runId);
        }

        /**
         * Get latest event for a run
         */
        public Uni<WorkflowEvent> getLatestEvent(String runId) {
                return find("runId = ?1", Sort.descending("sequence"), runId)
                                .firstResult()
                                .map(entity -> entity != null ? toEvent(entity) : null);
        }

        /**
         * Get next sequence number for run (atomic)
         */
        private Uni<Long> getNextSequence(String runId) {
                return getEntityManager()
                                .createQuery(
                                                "SELECT COALESCE(MAX(e.sequence), 0) + 1 FROM WorkflowEventEntity e WHERE e.runId = :runId",
                                                Long.class)
                                .setParameter("runId", runId)
                                .getSingleResult();
        }

        /**
         * Convert entity to event
         */
        private WorkflowEvent toEvent(WorkflowEventEntity entity) {
                return new WorkflowEvent(
                                entity.id,
                                entity.runId,
                                entity.sequence,
                                entity.type,
                                entity.data,
                                entity.createdAt);
        }
}
