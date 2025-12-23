package tech.kayys.wayang.workflow.service.backup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.model.BackupMetadata;

/**
 * Service for verifying backup integrity and consistency
 */
@ApplicationScoped
public class BackupVerificationService {

    private static final Logger log = LoggerFactory.getLogger(BackupVerificationService.class);

    @Inject
    BackupStorageService storageService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    CompressionService compressionService;

    /**
     * Verify backup integrity
     */
    public Uni<BackupVerificationResult> verifyBackup(String backupId, BackupMetadata metadata) {
        return verifyMetadata(metadata)
                .chain(metadataValid -> verifyFilesExist(metadata))
                .chain(filesExist -> verifyChecksums(metadata))
                .chain(checksumsValid -> verifyDataConsistency(metadata))
                .onItem().transform(allValid -> BackupVerificationResult.builder(backupId)
                        .valid(allValid)
                        .verifiedAt(Instant.now())
                        .build())
                .onFailure().recoverWithUni(error -> Uni.createFrom().item(
                        BackupVerificationResult.builder(backupId)
                                .valid(false)
                                .verifiedAt(Instant.now())
                                .addIssue("Verification failed: " + error.getMessage())
                                .build()));
    }

    /**
     * Verify backup metadata
     */
    private Uni<Boolean> verifyMetadata(BackupMetadata metadata) {
        List<String> issues = new ArrayList<>();

        if (metadata.getBackupId() == null || metadata.getBackupId().isEmpty()) {
            issues.add("Backup ID is missing");
        }

        if (metadata.getTimestamp() == null) {
            issues.add("Timestamp is missing");
        }

        if (metadata.getChecksum() == null || metadata.getChecksum().isEmpty()) {
            issues.add("Checksum is missing");
        }

        if (metadata.getBackupFiles() == null || metadata.getBackupFiles().isEmpty()) {
            issues.add("No backup files listed");
        }

        if (!issues.isEmpty()) {
            return Uni.createFrom().failure(new BackupVerificationException(
                    "Metadata verification failed: " + String.join(", ", issues)));
        }

        return Uni.createFrom().item(true);
    }

    /**
     * Verify all backup files exist in storage
     */
    private Uni<Boolean> verifyFilesExist(BackupMetadata metadata) {
        List<Uni<Boolean>> fileChecks = metadata.getBackupFiles().stream()
                .map(file -> storageService.getBackupSize(
                        metadata.getBackupId() + "/" + file.getFileName())
                        .onItem().transform(size -> size > 0)
                        .onFailure().recoverWithItem(false))
                .collect(Collectors.toList());

        return Uni.combine().all().unis(fileChecks)
                .combinedWith(results -> results.stream().allMatch(Boolean.class::cast));
    }

    /**
     * Verify file checksums
     */
    private Uni<Boolean> verifyChecksums(BackupMetadata metadata) {
        // Implementation would retrieve each file and verify its checksum
        return Uni.createFrom().item(true); // Placeholder
    }

    /**
     * Verify data consistency and referential integrity
     */
    private Uni<Boolean> verifyDataConsistency(BackupMetadata metadata) {
        // Implementation would verify that relationships between
        // workflow runs, events, and snapshots are consistent
        return Uni.createFrom().item(true); // Placeholder
    }

    /**
     * Verify backup can be restored
     */
    public Uni<Boolean> verifyRestoreCapability(String backupId) {
        return storageService.loadMetadata(backupId)
                .chain(metadata -> metadata.map(m -> verifyBackup(backupId, m)).orElse(
                        Uni.createFrom().item(BackupVerificationResult.failed(backupId, "Metadata not found"))))
                .onItem().transform(BackupVerificationResult::isValid);
    }

    /**
     * Verification result
     */
    public static class BackupVerificationResult {
        private final String backupId;
        private final boolean valid;
        private final Instant verifiedAt;
        private final List<String> issues;

        private BackupVerificationResult(Builder builder) {
            this.backupId = builder.backupId;
            this.valid = builder.valid;
            this.verifiedAt = builder.verifiedAt;
            this.issues = List.copyOf(builder.issues);
        }

        public String getBackupId() {
            return backupId;
        }

        public boolean isValid() {
            return valid;
        }

        public Instant getVerifiedAt() {
            return verifiedAt;
        }

        public List<String> getIssues() {
            return issues;
        }

        public static Builder builder(String backupId) {
            return new Builder(backupId);
        }

        public static BackupVerificationResult failed(String backupId, String reason) {
            return builder(backupId)
                    .valid(false)
                    .addIssue(reason)
                    .build();
        }

        public static class Builder {
            private final String backupId;
            private boolean valid = true;
            private Instant verifiedAt = Instant.now();
            private List<String> issues = new ArrayList<>();

            public Builder(String backupId) {
                this.backupId = backupId;
            }

            public Builder valid(boolean valid) {
                this.valid = valid;
                return this;
            }

            public Builder verifiedAt(Instant verifiedAt) {
                this.verifiedAt = verifiedAt;
                return this;
            }

            public Builder addIssue(String issue) {
                this.issues.add(issue);
                return this;
            }

            public Builder issues(List<String> issues) {
                this.issues = new ArrayList<>(issues);
                return this;
            }

            public BackupVerificationResult build() {
                return new BackupVerificationResult(this);
            }
        }
    }
}
