package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.backup.service.BackupManager;
import tech.kayys.wayang.workflow.backup.service.BackupManager.BackupOptions;
import tech.kayys.wayang.workflow.backup.service.BackupManager.BackupFilter;
import tech.kayys.wayang.workflow.model.RestoreOptions;
import tech.kayys.wayang.workflow.security.annotations.ControlPlaneSecured;

import tech.kayys.wayang.workflow.v1.*;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
@ControlPlaneSecured
public class BackupGrpcService implements BackupService {

    private static final Logger LOG = Logger.getLogger(BackupGrpcService.class);

    @Inject
    BackupManager backupManager;

    @Inject
    SecurityIdentity securityIdentity;

    private String getTenantId() {
        if (securityIdentity.isAnonymous()) {
            throw new IllegalStateException("Anonymous access not allowed");
        }
        return securityIdentity.getPrincipal().getName();
    }

    @Override
    public Uni<BackupResponse> createFullBackup(CreateBackupRequest request) {
        String tenantId = getTenantId();
        LOG.infof("Creating full backup for tenant: %s", tenantId);
        BackupOptions options = new BackupOptions(
                request.getEncrypt(),
                request.getCompress(),
                request.getMetadataMap());

        return backupManager.createFullBackup(options)
                .map(this::toProto);
    }

    @Override
    public Uni<RestoreResponse> restoreFromBackup(RestoreFromBackupRequest request) {
        RestoreOptions options = new RestoreOptions(
                request.getSelective(),
                request.getSelectedItemsList(),
                request.getSkipVerification(),
                new HashMap<>(request.getParametersMap()));

        return backupManager.restoreFromBackup(request.getBackupId(), options)
                .map(result -> RestoreResponse.newBuilder()
                        .setRestoreId(result.getBackupId())
                        .setStatus(result.getStatus().name())
                        .setMessage(result.getErrorMessage() != null ? result.getErrorMessage() : "")
                        .setRestoredAt(result.getRestoreTime() != null ? result.getRestoreTime().toEpochMilli() : 0)
                        .build());
    }

    @Override
    public Uni<BackupResponse> getBackup(GetBackupRequest request) {
        return backupManager.getBackupMetadata(request.getBackupId())
                .map(opt -> opt.map(this::toProto).orElse(null));
    }

    @Override
    public Uni<ListBackupsResponse> listBackups(ListBackupsRequest request) {
        tech.kayys.wayang.workflow.model.BackupType type = null;
        if (request.getType() != null && !request.getType().isEmpty()) {
            try {
                type = tech.kayys.wayang.workflow.model.BackupType.valueOf(request.getType());
            } catch (Exception e) {
            }
        }

        tech.kayys.wayang.workflow.model.BackupStatus status = null;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                status = tech.kayys.wayang.workflow.model.BackupStatus.valueOf(request.getStatus());
            } catch (Exception e) {
            }
        }

        BackupFilter filter = new BackupFilter(
                Optional.ofNullable(type),
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(status));

        return backupManager.listBackups(filter)
                .map(list -> ListBackupsResponse.newBuilder()
                        .addAllBackups(list.stream().map(this::toProto).collect(Collectors.toList()))
                        .setTotalCount(list.size())
                        .build());
    }

    @Override
    public Uni<VerifyBackupResponse> verifyBackup(GetBackupRequest request) {
        return backupManager.verifyBackup(request.getBackupId())
                .map(result -> VerifyBackupResponse.newBuilder()
                        .setBackupId(request.getBackupId())
                        .setValid(result.isValid())
                        .addAllIssues(result.getIssues())
                        .setVerifiedAt(System.currentTimeMillis())
                        .build());
    }

    @Override
    public Uni<RestoreResponse> pointInTimeRecovery(PointInTimeRecoveryRequest request) {
        return Uni.createFrom().item(RestoreResponse.newBuilder()
                .setStatus("FAILED")
                .setMessage("Not implemented in Adapter yet")
                .build());
    }

    @Override
    public Uni<CleanupResultResponse> cleanupOldBackups(com.google.protobuf.Empty request) {
        return backupManager.cleanupOldBackups()
                .map(result -> CleanupResultResponse.newBuilder()
                        .setDeletedCount(result.getDeletedCount())
                        .setFreedSpace(result.getFreedSpace())
                        .setCleanedAt(System.currentTimeMillis())
                        .build());
    }

    private BackupResponse toProto(tech.kayys.wayang.workflow.model.BackupMetadata domain) {
        if (domain == null)
            return null;
        return BackupResponse.newBuilder()
                .setBackupId(domain.getBackupId())
                .setType(domain.getBackupType().name())
                .setStatus(domain.getStatus().name())
                .setCreatedAt(domain.getTimestamp() != null ? domain.getTimestamp().toEpochMilli() : 0)
                .setSizeBytes(domain.getTotalSize())
                .build();
    }
}
