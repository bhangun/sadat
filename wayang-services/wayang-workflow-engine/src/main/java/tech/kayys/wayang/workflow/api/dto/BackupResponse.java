package tech.kayys.wayang.workflow.api.dto;

import java.time.Instant;
import lombok.Data;
import lombok.Builder;
import tech.kayys.wayang.workflow.model.BackupMetadata;
import tech.kayys.wayang.workflow.model.BackupType;
import tech.kayys.wayang.workflow.model.BackupStatus;

@Data
@Builder
public class BackupResponse {
    private String backupId;
    private Instant timestamp;
    private BackupType backupType;
    private long itemCount;
    private long totalSize;
    private String checksum;
    private BackupStatus status;
    private String storageLocation;
    private boolean encrypted;
    private boolean compressed;

    public static BackupResponse fromModel(BackupMetadata metadata) {
        return BackupResponse.builder()
                .backupId(metadata.getBackupId())
                .timestamp(metadata.getTimestamp())
                .backupType(metadata.getBackupType())
                .itemCount(metadata.getItemCount())
                .totalSize(metadata.getTotalSize())
                .checksum(metadata.getChecksum())
                .status(metadata.getStatus())
                .storageLocation(metadata.getStorageLocation())
                .encrypted(metadata.isEncrypted())
                .compressed(metadata.isCompressed())
                .build();
    }
}
