package tech.kayys.wayang.workflow.service.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.RequestBody;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import tech.kayys.wayang.workflow.model.BackupMetadata;
import tech.kayys.wayang.workflow.service.S3Client;
import tech.kayys.wayang.workflow.service.backup.BackupManager.BackupFilter;

/**
 * Service for storing backups to various storage providers
 */
@ApplicationScoped
public class BackupStorageService {

    private static final Logger log = LoggerFactory.getLogger(BackupStorageService.class);

    @ConfigProperty(name = "backup.storage.provider", defaultValue = "local")
    String storageProvider;

    @ConfigProperty(name = "backup.storage.base.path", defaultValue = "./backups")
    String baseStoragePath;

    @Inject
    @Any
    Instance<StorageProvider> storageProviders;

    private StorageProvider activeProvider;

    @PostConstruct
    void initialize() {
        activeProvider = selectStorageProvider();
        activeProvider.initialize();
        log.info("Backup storage initialized with provider: {}", storageProvider);
    }

    /**
     * Store file to backup storage
     */
    public Uni<Boolean> storeFile(String fileName, byte[] data) {
        return activeProvider.store(fileName, data)
                .onItem().invoke(success -> {
                    if (success) {
                        log.debug("File stored: {}", fileName);
                    }
                })
                .onFailure().recoverWithUni(error -> {
                    log.error("Failed to store file: {}", fileName, error);
                    return Uni.createFrom().item(false);
                });
    }

    /**
     * Store backup metadata
     */
    public Uni<Boolean> storeMetadata(BackupMetadata metadata) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            byte[] metadataBytes = mapper.writeValueAsBytes(metadata);
            String metadataPath = String.format("%s/metadata.json", metadata.getBackupId());

            return storeFile(metadataPath, metadataBytes)
                    .chain(success -> {
                        if (success) {
                            log.debug("Metadata stored for backup: {}", metadata.getBackupId());
                        }
                        return Uni.createFrom().item(success);
                    });
        } catch (Exception e) {
            log.error("Failed to serialize metadata for backup: {}", metadata.getBackupId(), e);
            return Uni.createFrom().item(false);
        }
    }

    /**
     * Load backup metadata
     */
    public Uni<Optional<BackupMetadata>> loadMetadata(String backupId) {
        String metadataPath = String.format("%s/metadata.json", backupId);

        return activeProvider.retrieve(metadataPath)
                .onItem().transform(data -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        BackupMetadata metadata = mapper.readValue(data, BackupMetadata.class);
                        return Optional.of(metadata);
                    } catch (Exception e) {
                        log.error("Failed to deserialize metadata for backup: {}", backupId, e);
                        return Optional.empty();
                    }
                })
                .onFailure().recoverWithItem(error -> {
                    log.error("Failed to load metadata for backup: {}", backupId, error);
                    return Optional.empty();
                });
    }

    /**
     * List backup metadata with filtering
     */
    public Uni<List<BackupMetadata>> listBackupMetadata(BackupFilter filter) {
        return activeProvider.listObjects("")
                .onItem().transform(files -> files.stream()
                        .filter(file -> file.endsWith("/metadata.json"))
                        .map(file -> {
                            String backupId = file.substring(0, file.lastIndexOf("/"));
                            return loadMetadata(backupId).await().indefinitely();
                        })
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(metadata -> matchesFilter(metadata, filter))
                        .sorted(Comparator.comparing(BackupMetadata::getTimestamp).reversed())
                        .collect(Collectors.toList()));
    }

    /**
     * Delete backup and all its files
     */
    public Uni<Long> deleteBackup(String backupId) {
        return activeProvider.listObjects(backupId + "/")
                .chain(files -> deleteFiles(files)
                        .onItem().transform(deletedCount -> {
                            log.info("Deleted backup {} with {} files", backupId, deletedCount);
                            return deletedCount;
                        }));
    }

    /**
     * Get backup size
     */
    public Uni<Long> getBackupSize(String backupId) {
        return activeProvider.listObjects(backupId + "/")
                .onItem().transform(files -> files.stream()
                        .mapToLong(file -> activeProvider.getFileSize(file).await().indefinitely())
                        .sum());
    }

    // Private implementation methods

    private StorageProvider selectStorageProvider() {
        return storageProviders.stream()
                .filter(provider -> provider.getProviderName().equals(storageProvider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No storage provider found for: " + storageProvider));
    }

    private boolean matchesFilter(BackupMetadata metadata, BackupFilter filter) {
        if (filter.type().isPresent() && metadata.getBackupType() != filter.type().get()) {
            return false;
        }

        if (filter.from().isPresent() && metadata.getTimestamp().isBefore(filter.from().get())) {
            return false;
        }

        if (filter.to().isPresent() && metadata.getTimestamp().isAfter(filter.to().get())) {
            return false;
        }

        if (filter.status().isPresent() && metadata.getStatus() != filter.status().get()) {
            return false;
        }

        return true;
    }

    private Uni<Long> deleteFiles(List<String> files) {
        List<Uni<Boolean>> deleteOperations = files.stream()
                .map(activeProvider::delete)
                .collect(Collectors.toList());

        return Uni.combine().all().unis(deleteOperations)
                .combinedWith(results -> results.stream()
                        .filter(Boolean.class::cast)
                        .count());
    }

    /**
     * Storage provider interface
     */
    public interface StorageProvider {
        String getProviderName();

        void initialize();

        Uni<Boolean> store(String path, byte[] data);

        Uni<byte[]> retrieve(String path);

        Uni<Boolean> delete(String path);

        Uni<List<String>> listObjects(String prefix);

        Uni<Long> getFileSize(String path);
    }

    /**
     * Local filesystem storage provider
     */
    @ApplicationScoped
    @Named("local")
    public static class LocalStorageProvider implements StorageProvider {

        private Path basePath;

        @Override
        public String getProviderName() {
            return "local";
        }

        @Override
        public void initialize() {
            try {
                basePath = Paths.get(System.getProperty("user.dir"), "backups");
                Files.createDirectories(basePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize local storage", e);
            }
        }

        @Override
        public Uni<Boolean> store(String path, byte[] data) {
            return Uni.createFrom().item(() -> {
                try {
                    Path filePath = basePath.resolve(path);
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, data);
                    return true;
                } catch (IOException e) {
                    log.error("Failed to store file locally: {}", path, e);
                    return false;
                }
            });
        }

        @Override
        public Uni<byte[]> retrieve(String path) {
            return Uni.createFrom().item(() -> {
                try {
                    Path filePath = basePath.resolve(path);
                    return Files.readAllBytes(filePath);
                } catch (IOException e) {
                    log.error("Failed to retrieve file: {}", path, e);
                    throw new RuntimeException("File retrieval failed", e);
                }
            });
        }

        @Override
        public Uni<Boolean> delete(String path) {
            return Uni.createFrom().item(() -> {
                try {
                    Path filePath = basePath.resolve(path);
                    return Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    log.error("Failed to delete file: {}", path, e);
                    return false;
                }
            });
        }

        @Override
        public Uni<List<String>> listObjects(String prefix) {
            return Uni.createFrom().item(() -> {
                try {
                    Path prefixPath = basePath.resolve(prefix);
                    return Files.walk(prefixPath)
                            .filter(Files::isRegularFile)
                            .map(basePath::relativize)
                            .map(Path::toString)
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    log.error("Failed to list objects with prefix: {}", prefix, e);
                    return List.of();
                }
            });
        }

        @Override
        public Uni<Long> getFileSize(String path) {
            return Uni.createFrom().item(() -> {
                try {
                    Path filePath = basePath.resolve(path);
                    return Files.size(filePath);
                } catch (IOException e) {
                    log.error("Failed to get file size: {}", path, e);
                    return 0L;
                }
            });
        }
    }

    /**
     * S3 storage provider (example implementation)
     */
    @ApplicationScoped
    @Named("s3")
    public static class S3StorageProvider implements StorageProvider {

        @Inject
        S3Client s3Client;

        @ConfigProperty(name = "backup.storage.s3.bucket")
        String bucketName;

        @Override
        public String getProviderName() {
            return "s3";
        }

        @Override
        public void initialize() {
            // Check if bucket exists, create if not
            log.info("S3 storage initialized with bucket: {}", bucketName);
        }

        @Override
        public Uni<Boolean> store(String path, byte[] data) {
            return Uni.createFrom().item(() -> {
                try {
                    PutObjectRequest request = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(path)
                            .contentLength((long) data.length)
                            .build();

                    s3Client.putObject(request, RequestBody.fromBytes(data));
                    return true;
                } catch (Exception e) {
                    log.error("Failed to store file to S3: {}", path, e);
                    return false;
                }
            });
        }

        // Other S3 implementation methods would go here
        // (retrieve, delete, listObjects, getFileSize)
    }
}
