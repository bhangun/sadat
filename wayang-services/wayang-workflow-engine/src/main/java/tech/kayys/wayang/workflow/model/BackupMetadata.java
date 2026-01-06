package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.List;

@lombok.Data
@lombok.Builder
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class BackupMetadata {
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
    private String baseBackupId;
    private Instant completedAt;
    private String errorMessage;
    private List<BackupFile> backupFiles;

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BackupFile {
        private String fileName;
        private long size;
        private String checksum;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String getBackupId() {
        return backupId;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public long getItemCount() {
        return itemCount;
    }

    public String getChecksum() {
        return checksum;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public String getBaseBackupId() {
        return baseBackupId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<BackupFile> getBackupFiles() {
        return backupFiles;
    }
}
