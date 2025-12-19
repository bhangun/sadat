package tech.kayys.wayang.workflow.model;

import java.time.Instant;

@lombok.Builder
public class BackupMetadata {
    private String backupId;
    private Instant timestamp;
    private BackupType backupType;
    private long itemCount;
    private String checksum;

    public String getBackupId() {
        return backupId;
    }

    public Instant getTimestamp() {
        return timestamp;
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
}
