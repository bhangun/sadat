package tech.kayys.wayang.workflow.backup.service;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

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
        log.info("Starting verification for backup: " + backupId);

        return Uni.combine().all().unis(
                verifyMetadata(metadata),
                verifyFilesExist(metadata),
                verifyChecksums(metadata),
                verifyDataConsistency(metadata)).with(results -> {
                    List<String> allIssues = new ArrayList<>();
                    for (Object result : results) {
                        if (result instanceof List) {
                            allIssues.addAll((List<String>) result);
                        }
                    }
                    return BackupVerificationResult.builder(backupId)
                            .valid(allIssues.isEmpty())
                            .issues(allIssues)
                            .verifiedAt(Instant.now())
                            .build();
                }).onFailure().recoverWithItem(error -> {
                    log.error("Verification crashed for backup " + backupId, error);
                    return BackupVerificationResult.builder(backupId)
                            .valid(false)
                            .verifiedAt(Instant.now())
                            .addIssue("Verification system error: " + error.getMessage())
                            .build();
                });
    }

    /**
     * Verify backup metadata
     */
    private Uni<List<String>> verifyMetadata(BackupMetadata metadata) {
        List<String> issues = new ArrayList<>();

        if (metadata == null) {
            issues.add("Metadata is null");
            return Uni.createFrom().item(issues);
        }

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

        return Uni.createFrom().item(issues);
    }

    /**
     * Verify all backup files exist in storage
     */
    private Uni<List<String>> verifyFilesExist(BackupMetadata metadata) {
        if (metadata == null || metadata.getBackupFiles() == null) {
            return Uni.createFrom().item(List.of());
        }

        log.info("Verifying files existence for backup: " + metadata.getBackupId());

        List<Uni<String>> fileChecks = metadata.getBackupFiles().stream()
                .map(file -> {
                    String filePath = metadata.getBackupId() + "/" + file.getFileName();
                    return storageService.getBackupSize(filePath)
                            .onItem().transform(size -> size > 0 ? null : "File not found or empty: " + filePath)
                            .onFailure()
                            .recoverWithItem(th -> "Error checking file " + filePath + ": " + th.getMessage());
                })
                .collect(Collectors.toList());

        return Uni.combine().all().unis(fileChecks)
                .with(results -> results.stream()
                        .filter(res -> res != null)
                        .map(Object::toString)
                        .collect(Collectors.toList()));
    }

    /**
     * Verify file checksums
     */
    private Uni<List<String>> verifyChecksums(BackupMetadata metadata) {
        if (metadata == null || metadata.getBackupFiles() == null) {
            return Uni.createFrom().item(List.of());
        }

        log.info("Verifying checksums for backup: " + metadata.getBackupId());

        List<Uni<String>> checksumChecks = metadata.getBackupFiles().stream()
                .map(file -> {
                    String filePath = metadata.getBackupId() + "/" + file.getFileName();
                    return storageService.retrieveFile(filePath)
                            .onItem().transform(data -> {
                                String calculated = calculateSHA256(data);
                                if (calculated.equalsIgnoreCase(file.getChecksum())) {
                                    return null;
                                }
                                return String.format("Checksum mismatch for %s: expected %s, got %s",
                                        file.getFileName(), file.getChecksum(), calculated);
                            })
                            .onFailure().recoverWithItem(th -> "Error reading file for checksum: " + file.getFileName()
                                    + " (" + th.getMessage() + ")");
                })
                .collect(Collectors.toList());

        return Uni.combine().all().unis(checksumChecks)
                .with(results -> results.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.toList()));
    }

    private Uni<List<String>> verifyDataConsistency(BackupMetadata metadata) {
        List<String> issues = new ArrayList<>();
        if (metadata == null) {
            return Uni.createFrom().item(issues);
        }

        if (metadata.getBackupFiles() == null) {
            issues.add("Backup files list is missing in metadata");
            return Uni.createFrom().item(issues);
        }

        long calculatedSize = metadata.getBackupFiles().stream()
                .mapToLong(tech.kayys.wayang.workflow.model.BackupMetadata.BackupFile::getSize)
                .sum();

        if (calculatedSize != metadata.getTotalSize()) {
            issues.add(String.format("Total size mismatch: metadata says %d bytes, but files sum up to %d bytes",
                    metadata.getTotalSize(), calculatedSize));
        }

        if (metadata.getBackupFiles().size() != metadata.getItemCount()) {
            issues.add(String.format("Item count mismatch: metadata says %d, but list contains %d files",
                    metadata.getItemCount(), metadata.getBackupFiles().size()));
        }

        return Uni.createFrom().item(issues);
    }

    private String calculateSHA256(byte[] data) {
        if (data == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return "error";
        }
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
