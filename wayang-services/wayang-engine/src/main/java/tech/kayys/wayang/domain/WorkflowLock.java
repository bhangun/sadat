package tech.kayys.wayang.domain;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * WorkflowLock - Concurrent editing lock
 */
@Entity
@Table(name = "workflow_locks", indexes = {
        @Index(name = "idx_lock_workflow", columnList = "workflow_id"),
        @Index(name = "idx_lock_expires", columnList = "expires_at")
})
public class WorkflowLock extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "workflow_id", nullable = false)
    public UUID workflowId;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "session_id", nullable = false)
    public String sessionId;

    @Column(name = "acquired_at", nullable = false)
    public Instant acquiredAt;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "heartbeat_at")
    public Instant heartbeatAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public LockType type;

    public enum LockType {
        EXCLUSIVE, // Full write lock
        SHARED // Read lock
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public static Uni<WorkflowLock> findActiveLock(UUID workflowId) {
        return find("workflowId = ?1 and expiresAt > ?2 order by acquiredAt desc",
                workflowId, Instant.now())
                .firstResult();
    }
}
