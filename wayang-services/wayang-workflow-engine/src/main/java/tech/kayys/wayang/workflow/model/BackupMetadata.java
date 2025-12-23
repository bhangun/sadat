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
}
