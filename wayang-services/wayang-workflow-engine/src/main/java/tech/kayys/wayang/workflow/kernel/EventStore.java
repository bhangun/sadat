package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.v1.WorkflowEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event store for workflow events with streaming capabilities
 */
@ApplicationScoped
public class EventStore {

    @Inject
    EventRepository eventRepository;

    @Inject
    EventSerializer eventSerializer;

    private final Map<String, List<WorkflowEvent>> inMemoryCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();

    public Uni<String> append(String runId, WorkflowEvent event) {
        return validateEvent(runId, event)
                .flatMap(validatedEvent -> {
                    // Update in-memory cache
                    inMemoryCache.computeIfAbsent(runId, k -> new ArrayList<>())
                            .add(validatedEvent);

                    // Update counter
                    eventCounters.computeIfAbsent(runId, k -> new AtomicLong())
                            .incrementAndGet();

                    // Persist to repository
                    return eventRepository.save(validatedEvent)
                            .onItem().invoke(savedId -> {
                                LOG.debug("Event saved for run {}: {}", runId, validatedEvent.type());
                            })
                            .onFailure().invoke(th -> {
                                // Rollback cache on failure
                                inMemoryCache.computeIfPresent(runId, (k, events) -> {
                                    if (!events.isEmpty()) {
                                        events.remove(events.size() - 1);
                                    }
                                    return events;
                                });
                                eventCounters.computeIfPresent(runId, (k, counter) -> {
                                    counter.decrementAndGet();
                                    return counter;
                                });
                            });
                });
    }

    public Uni<List<WorkflowEvent>> getEvents(String runId) {
        // Try cache first
        List<WorkflowEvent> cached = inMemoryCache.get(runId);
        if (cached != null && !cached.isEmpty()) {
            return Uni.createFrom().item(List.copyOf(cached));
        }

        // Fall back to repository
        return eventRepository.findByRunId(runId)
                .onItem().invoke(events -> {
                    // Populate cache
                    if (events != null && !events.isEmpty()) {
                        inMemoryCache.put(runId, new ArrayList<>(events));
                        eventCounters.put(runId, new AtomicLong(events.size()));
                    }
                });
    }

    public Uni<List<WorkflowEvent>> getEventsAfter(String runId, long fromEventCount) {
        return getEvents(runId)
                .map(events -> {
                    if (events == null || events.size() <= fromEventCount) {
                        return List.of();
                    }
                    return events.subList((int) fromEventCount, events.size());
                });
    }

    public Multi<WorkflowEvent> streamEvents(String runId) {
        return Multi.createFrom().emitter(emitter -> {
            // First emit cached events
            List<WorkflowEvent> cached = inMemoryCache.get(runId);
            if (cached != null) {
                cached.forEach(emitter::emit);
            }

            // Then stream new events
            // This would typically use a message broker or similar
            // For now, we'll just complete the stream
            emitter.complete();
        });
    }

    public Uni<Long> getEventCount(String runId) {
        AtomicLong counter = eventCounters.get(runId);
        if (counter != null) {
            return Uni.createFrom().item(counter.get());
        }

        return eventRepository.countByRunId(runId)
                .onItem().invoke(count -> {
                    eventCounters.put(runId, new AtomicLong(count));
                });
    }

    public Uni<Void> replayEvents(String runId, EventReplayHandler handler) {
        return getEvents(runId)
                .onItem().transformToMulti(events -> Multi.createFrom().iterable(events))
                .onItem().transformToUniAndConcatenate(event -> handler.handle(event))
                .collect().asList()
                .replaceWithVoid();
    }

    public Uni<Void> compactEvents(String runId, CompactionStrategy strategy) {
        return getEvents(runId)
                .flatMap(events -> {
                    List<WorkflowEvent> compacted = strategy.compact(events);

                    // Update cache
                    inMemoryCache.put(runId, new ArrayList<>(compacted));
                    eventCounters.put(runId, new AtomicLong(compacted.size()));

                    // Persist compaction
                    return eventRepository.replaceEvents(runId, compacted);
                });
    }

    public Uni<WorkflowEvent> getLastEvent(String runId) {
        return getEvents(runId)
                .map(events -> {
                    if (events == null || events.isEmpty()) {
                        return null;
                    }
                    return events.get(events.size() - 1);
                });
    }

    public Uni<List<WorkflowEvent>> getEventsByType(String runId, String eventType) {
        return getEvents(runId)
                .map(events -> events.stream()
                        .filter(e -> e.type().equals(eventType))
                        .toList());
    }

    public Uni<Map<String, Long>> getEventStatistics(String runId) {
        return getEvents(runId)
                .map(events -> {
                    Map<String, Long> stats = new java.util.HashMap<>();
                    events.forEach(event -> stats.merge(event.type(), 1L, Long::sum));
                    return Map.copyOf(stats);
                });
    }

    private Uni<WorkflowEvent> validateEvent(String runId, WorkflowEvent event) {
        return Uni.createFrom().deferred(() -> {
            if (event == null) {
                return Uni.createFrom().failure(new IllegalArgumentException("Event cannot be null"));
            }

            if (runId == null || runId.trim().isEmpty()) {
                return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
            }

            if (!runId.equals(event.runId())) {
                return Uni.createFrom().failure(new IllegalArgumentException(
                        String.format("Run ID mismatch: expected %s, got %s", runId, event.runId())));
            }

            if (event.type() == null || event.type().trim().isEmpty()) {
                return Uni.createFrom().failure(new IllegalArgumentException("Event type cannot be null or empty"));
            }

            if (event.timestamp() == null) {
                // Set timestamp if missing
                WorkflowEvent corrected = WorkflowEvent.builder()
                        .runId(event.runId())
                        .type(event.type())
                        .timestamp(Instant.now())
                        .data(event.data() != null ? Map.copyOf(event.data()) : Map.of())
                        .build();
                return Uni.createFrom().item(corrected);
            }

            return Uni.createFrom().item(event);
        });
    }

    public interface EventReplayHandler {
        Uni<Void> handle(WorkflowEvent event);
    }

    public interface CompactionStrategy {
        List<WorkflowEvent> compact(List<WorkflowEvent> events);
    }

    public static class SnapshotCompactionStrategy implements CompactionStrategy {
        private final int snapshotInterval;

        public SnapshotCompactionStrategy(int snapshotInterval) {
            this.snapshotInterval = snapshotInterval;
        }

        @Override
        public List<WorkflowEvent> compact(List<WorkflowEvent> events) {
            if (events.size() <= snapshotInterval) {
                return events; // No compaction needed
            }

            List<WorkflowEvent> compacted = new ArrayList<>();
            int snapshotIndex = events.size() - (events.size() % snapshotInterval);

            // Keep all events after the last snapshot point
            for (int i = snapshotIndex; i < events.size(); i++) {
                compacted.add(events.get(i));
            }

            // Add a snapshot event summarizing the compacted events
            if (snapshotIndex > 0) {
                compacted.add(0, createSnapshotEvent(events.subList(0, snapshotIndex)));
            }

            return compacted;
        }

        private WorkflowEvent createSnapshotEvent(List<WorkflowEvent> events) {
            Map<String, Object> snapshotData = Map.of(
                    "compactedEvents", events.size(),
                    "firstEventTimestamp", events.get(0).timestamp().toString(),
                    "lastEventTimestamp", events.get(events.size() - 1).timestamp().toString(),
                    "eventTypes", events.stream()
                            .map(WorkflowEvent::type)
                            .distinct()
                            .toList());

            return WorkflowEvent.builder()
                    .runId(events.get(0).runId())
                    .type("SNAPSHOT")
                    .timestamp(Instant.now())
                    .data(snapshotData)
                    .build();
        }
    }
}
