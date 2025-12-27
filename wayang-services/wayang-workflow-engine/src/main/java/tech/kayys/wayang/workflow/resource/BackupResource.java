package tech.kayys.wayang.workflow.resource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;

import org.jboss.logging.Logger;

import tech.kayys.wayang.workflow.api.dto.BackupRequest;
import tech.kayys.wayang.workflow.api.dto.RestoreRequest;
import tech.kayys.wayang.workflow.api.dto.BackupResponse;
import tech.kayys.wayang.workflow.api.dto.RestoreResponse;
import tech.kayys.wayang.workflow.api.dto.ListBackupsResponse;
import tech.kayys.wayang.workflow.api.dto.VerifyBackupResponse;
import tech.kayys.wayang.workflow.api.dto.CleanupResultResponse;
import tech.kayys.wayang.workflow.api.dto.ErrorResponse;
import tech.kayys.wayang.workflow.service.backup.BackupManager;
import tech.kayys.wayang.workflow.service.backup.BackupManager.BackupOptions;
import tech.kayys.wayang.workflow.service.backup.BackupManager.BackupFilter;
import tech.kayys.wayang.workflow.model.BackupType;
import tech.kayys.wayang.workflow.model.BackupStatus;
import tech.kayys.wayang.workflow.model.RestoreOptions;
import tech.kayys.wayang.workflow.exception.BackupCorruptedException;

/**
 * Backup and Restore API Resource
 * 
 * Provides endpoints for backup and restore operations:
 * - Create full/incremental backups
 * - Restore from backups
 * - List available backups
 * - Verify backup integrity
 * - Perform cleanup operations
 */
@Path("/api/v1/backups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Backup & Restore", description = "Workflow backup and restore operations")
public class BackupResource {

        private static final Logger log = Logger.getLogger(BackupResource.class);

        @Inject
        BackupManager backupManager;

        @Context
        SecurityContext securityContext;

        @Context
        HttpServerRequest request;

        /**
         * Create a new full backup
         */
        @POST
        @Operation(summary = "Create full backup", description = "Create a new full backup of all workflow data")
        @RolesAllowed({ "admin", "backup_operator" })
        public Uni<Response> createFullBackup(BackupRequest request) {
                log.infof("Creating full backup by user: %s", getUserId());

                BackupOptions options = BackupOptions.defaultOptions();
                if (request != null) {
                        options = new BackupOptions(
                                        request.getEncrypt() != null ? request.getEncrypt() : false,
                                        request.getCompress() != null ? request.getCompress() : false,
                                        request.getMetadata() != null ? request.getMetadata() : java.util.Map.of());
                }

                return backupManager.createFullBackup(options)
                                .map(metadata -> Response.status(Response.Status.CREATED)
                                                .entity(BackupResponse.fromModel(metadata))
                                                .build())
                                .onFailure().recoverWithItem(th -> {
                                        log.error("Failed to create backup", th);
                                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity(ErrorResponse.builder()
                                                                        .errorCode("BACKUP_CREATE_FAILED")
                                                                        .message(th.getMessage())
                                                                        .timestamp(Instant.now())
                                                                        .build())
                                                        .build();
                                });
        }

        /**
         * Restore from a backup
         */
        @POST
        @Path("/{backupId}/restore")
        @Operation(summary = "Restore from backup", description = "Restore workflow data from a specific backup")
        @RolesAllowed({ "admin", "restore_operator" })
        public Uni<Response> restoreFromBackup(
                        @PathParam("backupId") String backupId,
                        RestoreRequest request) {

                log.infof("Restoring from backup: %s by user: %s", backupId, getUserId());

                RestoreOptions options = RestoreOptions.fullRestore();
                if (request != null) {
                        options = new RestoreOptions(
                                        request.getSelective() != null ? request.getSelective() : false,
                                        request.getSelectedItems() != null ? request.getSelectedItems()
                                                        : java.util.List.of(),
                                        request.getSkipVerification() != null ? request.getSkipVerification() : false,
                                        request.getParameters() != null ? request.getParameters() : java.util.Map.of());
                }

                return backupManager.restoreFromBackup(backupId, options)
                                .map(result -> Response.status(Response.Status.OK)
                                                .entity(RestoreResponse.fromModel(result))
                                                .build())
                                .onFailure().recoverWithItem(th -> {
                                        log.errorf("Failed to restore from backup: %s", backupId, th);
                                        return Response.status(isBackupCorrupted(th) ? Response.Status.CONFLICT
                                                        : Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity(ErrorResponse.builder()
                                                                        .errorCode(isBackupCorrupted(th)
                                                                                        ? "BACKUP_CORRUPTED"
                                                                                        : "RESTORE_FAILED")
                                                                        .message(th.getMessage())
                                                                        .timestamp(Instant.now())
                                                                        .build())
                                                        .build();
                                });
        }

        /**
         * Get backup details by ID
         */
        @GET
        @Path("/{backupId}")
        @Operation(summary = "Get backup details", description = "Retrieve detailed information about a specific backup")
        @RolesAllowed({ "admin", "backup_operator", "viewer" })
        public Uni<Response> getBackup(@PathParam("backupId") String backupId) {
                return backupManager.getBackupMetadata(backupId)
                                .map(metadataOpt -> {
                                        if (metadataOpt.isPresent()) {
                                                return Response.ok(BackupResponse.fromModel(metadataOpt.get())).build();
                                        } else {
                                                return Response.status(Response.Status.NOT_FOUND)
                                                                .entity(ErrorResponse.builder()
                                                                                .errorCode("BACKUP_NOT_FOUND")
                                                                                .message("Backup not found: "
                                                                                                + backupId)
                                                                                .timestamp(Instant.now())
                                                                                .build())
                                                                .build();
                                        }
                                })
                                .onFailure().recoverWithItem(th -> {
                                        log.errorf("Failed to get backup: %s", backupId, th);
                                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity(ErrorResponse.builder()
                                                                        .errorCode("GET_BACKUP_FAILED")
                                                                        .message(th.getMessage())
                                                                        .timestamp(Instant.now())
                                                                        .build())
                                                        .build();
                                });
        }

        /**
         * List all backups with optional filtering
         */
        @GET
        @Operation(summary = "List backups", description = "List all available backups with optional filters")
        @RolesAllowed({ "admin", "backup_operator", "viewer" })
        public Uni<Response> listBackups(
                        @QueryParam("type") String type,
                        @QueryParam("from") String from,
                        @QueryParam("to") String to,
                        @QueryParam("status") String status,
                        @QueryParam("limit") @DefaultValue("20") int limit) {

                Optional<BackupType> typeFilter = Optional.empty();
                Optional<Instant> fromFilter = Optional.empty();
                Optional<Instant> toFilter = Optional.empty();
                Optional<BackupStatus> statusFilter = Optional.empty();

                if (type != null) {
                        try {
                                typeFilter = java.util.Optional.of(tech.kayys.wayang.workflow.model.BackupType
                                                .valueOf(type.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder()
                                                                .errorCode("INVALID_TYPE")
                                                                .message("Invalid backup type: " + type)
                                                                .timestamp(Instant.now())
                                                                .build())
                                                .build());
                        }
                }

                if (from != null) {
                        try {
                                fromFilter = java.util.Optional.of(Instant.parse(from));
                        } catch (Exception e) {
                                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder()
                                                                .errorCode("INVALID_DATE_FORMAT")
                                                                .message("Invalid date format for 'from': " + from)
                                                                .timestamp(Instant.now())
                                                                .build())
                                                .build());
                        }
                }

                if (to != null) {
                        try {
                                toFilter = java.util.Optional.of(Instant.parse(to));
                        } catch (Exception e) {
                                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder()
                                                                .errorCode("INVALID_DATE_FORMAT")
                                                                .message("Invalid date format for 'to': " + to)
                                                                .timestamp(Instant.now())
                                                                .build())
                                                .build());
                        }
                }

                if (status != null) {
                        try {
                                statusFilter = java.util.Optional.of(BackupStatus.valueOf(status.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder()
                                                                .errorCode("INVALID_STATUS")
                                                                .message("Invalid backup status: " + status)
                                                                .timestamp(Instant.now())
                                                                .build())
                                                .build());
                        }
                }

                BackupFilter filter = new BackupFilter(typeFilter, fromFilter, toFilter, statusFilter);

                return backupManager.listBackups(filter)
                                .map(backups -> {
                                        List<BackupResponse> backupResponses = backups.stream()
                                                        .map(BackupResponse::fromModel)
                                                        .limit(limit)
                                                        .toList();

                                        return Response.ok(ListBackupsResponse.builder()
                                                        .backups(backupResponses)
                                                        .totalCount(backupResponses.size())
                                                        .build()).build();
                                })
                                .onFailure().recoverWithItem(th -> {
                                        log.error("Failed to list backups", th);
                                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity(ErrorResponse.builder()
                                                                        .errorCode("LIST_BACKUPS_FAILED")
                                                                        .message(th.getMessage())
                                                                        .timestamp(Instant.now())
                                                                        .build())
                                                        .build();
                                });
        }

        /**
         * Verify backup integrity
         */
        @POST
        @Path("/{backupId}/verify")
        @Operation(summary = "Verify backup", description = "Verify the integrity of a specific backup")
        @RolesAllowed({ "admin", "backup_operator", "viewer" })
        public Uni<Response> verifyBackup(@PathParam("backupId") String backupId) {
                log.infof("Verifying backup: %s", backupId);

                return backupManager.verifyBackup(backupId)
                                .map(verificationResult -> Response.ok(VerifyBackupResponse.builder()
                                                .backupId(backupId)
                                                .valid(verificationResult.isValid())
                                                .issues(verificationResult.getIssues())
                                                .verifiedAt(Instant.now())
                                                .build()).build())
                                .onFailure().recoverWithItem(th -> {
                                        log.errorf("Failed to verify backup: %s", backupId, th);
                                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity(ErrorResponse.builder()
                                                                        .errorCode("VERIFY_BACKUP_FAILED")
                                                                        .message(th.getMessage())
                                                                        .timestamp(Instant.now())
                                                                        .build())
                                                        .build();
                                });
        }

        /**
         * Perform point-in-time recovery
         */
        @POST
        @Path("/{backupId}/point-in-time-recovery")
        @Operation(summary = "Point-in-time recovery", description = "Perform point-in-time recovery from a backup to a specific time")
        @RolesAllowed({ "admin", "restore_operator" })
        public Uni<Response> pointInTimeRecovery(
                        @PathParam("backupId") String backupId,
                        @QueryParam("targetTime") String targetTimeString) {

                log.infof("Starting point-in-time recovery from backup: %s to time: %s", backupId, targetTimeString);

                try {
                        Instant targetTime = Instant.parse(targetTimeString);

                        RestoreOptions options = RestoreOptions.fullRestore();
                        return backupManager.performPointInTimeRecovery(backupId, targetTime, options)
                                        .map(result -> Response.ok(RestoreResponse.fromModel(result)).build())
                                        .onFailure().recoverWithItem(th -> {
                                                log.errorf("Failed point-in-time recovery: %s", backupId, th);
                                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                                .entity(ErrorResponse.builder()
                                                                                .errorCode("PITR_FAILED")
                                                                                .message(th.getMessage())
                                                                                .timestamp(Instant.now())
                                                                                .build())
                                                                .build();
                                        });
                } catch (Exception e) {
                        return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                        .entity(ErrorResponse.builder()
                                                        .errorCode("INVALID_TARGET_TIME")
                                                        .message("Invalid target time format: " + targetTimeString)
                                                        .timestamp(Instant.now())
                                                        .build())
                                        .build());
                }
        }

        /**
         * Cleanup old backups based on retention policy
         */
        @POST
        @Path("/cleanup")
        @Operation(summary = "Cleanup old backups", description = "Remove old backups based on retention policy")
        @RolesAllowed({ "admin", "backup_operator" })
        public Uni<Response> cleanupOldBackups() {
                log.infof("Starting cleanup of old backups by user: %s", getUserId());

                return backupManager.cleanupOldBackups()
                                .map(result -> Response.ok(CleanupResultResponse.builder()
                                                .deletedCount(result.getDeletedCount())
                                                .freedSpace(result.getFreedSpace())
                                                .cleanedAt(Instant.now())
                                                .build()).build())
                                .onFailure().recoverWithItem(th -> {
                                        log.error("Failed to cleanup old backups", th);
                                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity(ErrorResponse.builder()
                                                                        .errorCode("CLEANUP_FAILED")
                                                                        .message(th.getMessage())
                                                                        .timestamp(Instant.now())
                                                                        .build())
                                                        .build();
                                });
        }

        // Helper methods
        private String getUserId() {
                return securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName()
                                : "unknown";
        }

        private boolean isBackupCorrupted(Throwable th) {
                return th instanceof BackupCorruptedException;
        }
}