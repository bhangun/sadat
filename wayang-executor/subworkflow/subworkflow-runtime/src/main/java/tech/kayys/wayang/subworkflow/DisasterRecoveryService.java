package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Disaster Recovery
 */
interface DisasterRecoveryService {

    /**
     * Create backup
     */
    Uni<BackupId> createBackup(
        tech.kayys.silat.core.domain.TenantId tenantId,
        BackupScope scope
    );

    /**
     * Restore from backup
     */
    Uni<Void> restoreFromBackup(
        BackupId backupId,
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Setup cross-region replication
     */
    Uni<Void> setupReplication(
        String primaryRegion,
        String secondaryRegion,
        ReplicationConfig config
    );

    /**
     * Failover to secondary region
     */
    Uni<Void> failover(
        String toRegion,
        FailoverMode mode // MANUAL, AUTOMATIC
    );
}