package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Encryption
 */
interface WorkflowEncryptionService {

    /**
     * Encrypt sensitive workflow data
     */
    Uni<EncryptedData> encrypt(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        String data,
        EncryptionConfig config
    );

    /**
     * Decrypt workflow data
     */
    Uni<String> decrypt(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        EncryptedData encryptedData
    );

    /**
     * Rotate encryption keys
     */
    Uni<Void> rotateKeys(tech.kayys.silat.core.domain.TenantId tenantId);
}