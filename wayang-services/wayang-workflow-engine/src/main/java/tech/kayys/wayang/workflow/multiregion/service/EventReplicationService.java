package tech.kayys.wayang.workflow.multiregion.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.api.model.WorkflowEvent;
import tech.kayys.wayang.workflow.multiregion.service.MultiRegionCoordinator.ReplicationMode;
import tech.kayys.wayang.workflow.multiregion.service.MultiRegionCoordinator.ReplicationResult;

/**
 * Event replication service with different consistency modes
 */
/**
 * Event replication service with different consistency modes
 */
@ApplicationScoped
public class EventReplicationService {

    private static final Logger log = LoggerFactory.getLogger(EventReplicationService.class);

    @ConfigProperty(name = "app.replication.max.retries", defaultValue = "3")
    int maxRetries;

    @ConfigProperty(name = "app.replication.timeout.seconds", defaultValue = "30")
    long replicationTimeoutSeconds;

    @ConfigProperty(name = "app.replication.batch.size", defaultValue = "100")
    int batchSize;

    @Inject
    @Any
    Instance<ReplicationTransport> replicationTransports;

    @Inject
    ReplicationMetrics metrics;

    private final Map<ReplicationMode, ReplicationStrategy> strategies = new ConcurrentHashMap<>();

    @PostConstruct
    void initialize() {
        registerStrategies();
        log.info("EventReplicationService initialized");
    }

    /**
     * Replicate event to target regions
     */
    public Uni<ReplicationResult> replicate(WorkflowEvent event, ReplicationMode mode) {
        if (event == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Event cannot be null"));
        }

        ReplicationStrategy strategy = strategies.getOrDefault(mode, strategies.get(ReplicationMode.ASYNC));

        return strategy.replicate(event)
                .onItem().invoke(result -> {
                    // Assuming event.type() returns String or we need to convert it
                    String eventType = event.type().toString();
                    metrics.recordReplication(eventType, mode, result.successful());
                })
                .onFailure().recoverWithUni(ex -> handleReplicationFailure(event, mode, ex));
    }

    /**
     * Batch replicate multiple events
     */
    public Uni<BatchReplicationResult> replicateBatch(List<WorkflowEvent> events, ReplicationMode mode) {
        if (events == null || events.isEmpty()) {
            return Uni.createFrom().item(BatchReplicationResult.empty());
        }

        return Uni.combine().all().unis(
                events.stream()
                        .map(event -> replicate(event, mode))
                        .collect(Collectors.toList()))
                .with(results -> {
                    List<ReplicationResult> resultList = results.stream()
                            .map(ReplicationResult.class::cast)
                            .collect(Collectors.toList());

                    long successful = resultList.stream().filter(ReplicationResult::successful).count();
                    return new BatchReplicationResult(
                            resultList,
                            successful,
                            events.size() - successful);
                });
    }

    // Private implementation methods

    private void registerStrategies() {
        strategies.put(ReplicationMode.SYNC, new SynchronousReplicationStrategy());
        strategies.put(ReplicationMode.ASYNC, new AsynchronousReplicationStrategy());
        strategies.put(ReplicationMode.BATCH, new BatchReplicationStrategy(batchSize));
        strategies.put(ReplicationMode.LAZY, new LazyReplicationStrategy());
    }

    private Uni<ReplicationResult> handleReplicationFailure(WorkflowEvent event, ReplicationMode mode, Throwable ex) {
        log.error("Replication failed for event: {}, mode: {}", event.id(), mode, ex);

        return Uni.createFrom().item(
                ReplicationResult.failed("Replication failed: " + ex.getMessage()));
    }

    // Replication strategies

    public interface ReplicationStrategy {
        Uni<ReplicationResult> replicate(WorkflowEvent event);
    }

    public class SynchronousReplicationStrategy implements ReplicationStrategy {
        @Override
        public Uni<ReplicationResult> replicate(WorkflowEvent event) {
            // Synchronous replication - wait for confirmation from all regions
            return Uni.createFrom().item(() -> new ReplicationResult(
                    event.id(),
                    Set.of("us-east-1", "us-west-2"), // Mock replicated regions
                    ReplicationMode.SYNC,
                    true,
                    Optional.empty()));
        }
    }

    public class AsynchronousReplicationStrategy implements ReplicationStrategy {
        @Override
        public Uni<ReplicationResult> replicate(WorkflowEvent event) {
            // Asynchronous replication - fire and forget
            return Uni.createFrom().item(() -> new ReplicationResult(
                    event.id(),
                    Set.of("us-east-1"),
                    ReplicationMode.ASYNC,
                    true,
                    Optional.empty()));
        }
    }

    public class BatchReplicationStrategy implements ReplicationStrategy {
        private final List<WorkflowEvent> batchBuffer = new ArrayList<>();
        private final int batchSize;

        public BatchReplicationStrategy(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public Uni<ReplicationResult> replicate(WorkflowEvent event) {
            batchBuffer.add(event);

            if (batchBuffer.size() >= batchSize) {
                return flushBatch();
            }

            // Store in buffer, will be replicated later
            return Uni.createFrom().item(() -> new ReplicationResult(
                    event.id(),
                    Set.of(),
                    ReplicationMode.BATCH,
                    true,
                    Optional.of("Event batched, will be replicated later")));
        }

        private Uni<ReplicationResult> flushBatch() {
            // In real implementation, this would replicate the entire batch
            List<String> eventIds = batchBuffer.stream()
                    .map(WorkflowEvent::id)
                    .collect(Collectors.toList());

            batchBuffer.clear();

            return Uni.createFrom().item(() -> new ReplicationResult(
                    "batch-" + System.currentTimeMillis(),
                    Set.of("us-east-1", "eu-west-1"),
                    ReplicationMode.BATCH,
                    true,
                    Optional.of("Batch of " + eventIds.size() + " events replicated")));
        }
    }

    public class LazyReplicationStrategy implements ReplicationStrategy {
        @Override
        public Uni<ReplicationResult> replicate(WorkflowEvent event) {
            // Lazy replication - only replicate on demand
            return Uni.createFrom().item(() -> new ReplicationResult(
                    event.id(),
                    Set.of(),
                    ReplicationMode.LAZY,
                    true,
                    Optional.of("Event stored locally, will replicate on demand")));
        }
    }

    /**
     * Interface for different transport mechanisms
     */
    public interface ReplicationTransport {
        Uni<TransportResult> send(WorkflowEvent event, String targetRegion);
    }

    @ApplicationScoped
    public static class ReplicationMetrics {
        private final MeterRegistry meterRegistry;
        private final Counter successCounter;
        private final Counter failureCounter;

        @Inject
        public ReplicationMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            // Initialize counters after meterRegistry is set
            this.successCounter = Counter.builder("replication.success")
                    .description("Successful replications")
                    .register(meterRegistry);
            this.failureCounter = Counter.builder("replication.failure")
                    .description("Failed replications")
                    .register(meterRegistry);
        }

        public void recordReplication(String eventType, ReplicationMode mode, boolean success) {
            if (success) {
                successCounter.increment();
            } else {
                failureCounter.increment();
            }

            // Tagged counter for more detailed metrics
            Counter.builder("replication.attempts")
                    .tag("eventType", eventType)
                    .tag("mode", mode.name())
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry)
                    .increment();
        }
    }

    public record TransportResult(
            String region,
            boolean success,
            long durationMillis,
            Optional<String> errorMessage) {
    }

    public record BatchReplicationResult(
            List<ReplicationResult> individualResults,
            long successfulCount,
            long failedCount) {
        public static BatchReplicationResult empty() {
            return new BatchReplicationResult(List.of(), 0, 0);
        }
    }
}
