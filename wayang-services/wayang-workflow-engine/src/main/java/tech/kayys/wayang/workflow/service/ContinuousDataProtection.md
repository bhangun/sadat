package tech.kayys.wayang.workflow.service;

import java.time.Duration;
import java.time.Instant;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.api.model.WorkflowEvent;

/**
 * Continuous Data Protection (CDP)
 */
@ApplicationScoped
public class ContinuousDataProtection {

    @Inject
    EventBus eventBus;

    @Inject
    ReplicationJournal journal;

    /**
     * Capture all changes in real-time
     */
    @ConsumeEvent("workflow.events")
    void captureChange(WorkflowEvent event) {
        // Write to replication journal
        journal.append(new JournalEntry(
                event.id(),
                event.type(),
                event.data(),
                Instant.now())).subscribe().with(
                        v -> {
                        },
                        error -> log.error("Failed to journal event", error));
    }

    /**
     * Recovery point objective (RPO) monitoring
     */
    @Scheduled(every = "1m")
    void monitorRPO() {
        Instant lastReplicated = journal.getLastReplicatedTimestamp();
        Duration rpoAge = Duration.between(lastReplicated, Instant.now());

        if (rpoAge.toMinutes() > 5) { // RPO = 5 minutes
            log.error("RPO violated! Last replication: {} minutes ago",
                    rpoAge.toMinutes());
            alertOps("RPO violation detected");
        }
    }
}