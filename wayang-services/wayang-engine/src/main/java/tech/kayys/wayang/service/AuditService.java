package tech.kayys.wayang.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.model.NodeStatus;
import tech.kayys.wayang.schema.ExecutionStatus;

/**
 * Service for audit logging and provenance tracking.
 * 
 * Responsibilities:
 * - Record all significant events
 * - Generate tamper-proof audit trails
 * - Provide cryptographic hashing
 * - Support compliance requirements
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuditService {

        private static final Logger log = LoggerFactory.getLogger(AuditService.class);

        @Inject
        AuditLogger auditLogger;

        @Inject
        ProvenanceTracker provenanceTracker;

        /**
         * Audit run state transition.
         */
        public Uni<Void> auditRunStateTransition(
                        UUID runId,
                        ExecutionStatus fromStatus,
                        ExecutionStatus toStatus) {
                AuditPayload payload = AuditPayload.builder()
                                .runId(runId)
                                .event("RUN_STATE_TRANSITION")
                                .level(AuditPayload.AuditLevel.INFO)
                                .actor(AuditPayload.Actor.builder()
                                                .type(AuditPayload.ActorType.SYSTEM)
                                                .id("orchestrator")
                                                .build())
                                .metadata(java.util.Map.of(
                                                "fromStatus", fromStatus,
                                                "toStatus", toStatus))
                                .build();

                return auditLogger.log(payload);
        }

        /**
         * Audit node state transition.
         */
        public Uni<Void> auditNodeStateTransition(
                        UUID runId,
                        String nodeId,
                        NodeStatus fromStatus,
                        NodeStatus toStatus) {
                AuditPayload payload = AuditPayload.builder()
                                .runId(runId)
                                .nodeId(nodeId)
                                .event("NODE_STATE_TRANSITION")
                                .level(AuditPayload.AuditLevel.INFO)
                                .actor(AuditPayload.Actor.builder()
                                                .type(AuditPayload.ActorType.SYSTEM)
                                                .id("orchestrator")
                                                .build())
                                .metadata(java.util.Map.of(
                                                "fromStatus", fromStatus,
                                                "toStatus", toStatus))
                                .build();

                return auditLogger.log(payload);
        }

        /**
         * Audit human task creation.
         */
        public Uni<Void> auditHumanTaskCreated(HumanTask task) {
                AuditPayload payload = AuditPayload.builder()
                                .runId(task.getRunId())
                                .nodeId(task.getNodeId())
                                .event("HITL_TASK_CREATED")
                                .level(AuditPayload.AuditLevel.INFO)
                                .actor(AuditPayload.Actor.builder()
                                                .type(AuditPayload.ActorType.SYSTEM)
                                                .id("orchestrator")
                                                .build())
                                .metadata(java.util.Map.of(
                                                "taskId", task.getTaskId(),
                                                "title", task.getTitle(),
                                                "priority", task.getPriority()))
                                .build();

                return auditLogger.log(payload);
        }

        /**
         * Audit human task completion.
         */
        public Uni<Void> auditHumanTaskCompleted(HumanTask task) {
                AuditPayload payload = AuditPayload.builder()
                                .runId(task.getRunId())
                                .nodeId(task.getNodeId())
                                .event("HITL_TASK_COMPLETED")
                                .level(AuditPayload.AuditLevel.INFO)
                                .actor(AuditPayload.Actor.builder()
                                                .type(AuditPayload.ActorType.HUMAN)
                                                .id(task.getCompletedBy())
                                                .role("operator")
                                                .build())
                                .metadata(java.util.Map.of(
                                                "taskId", task.getTaskId(),
                                                "decision", task.getDecision() != null ? task.getDecision() : "none",
                                                "notes", task.getNotes() != null ? task.getNotes() : ""))
                                .build();

                return auditLogger.log(payload);
        }

        /**
         * Audit error event.
         */
        public Uni<Void> auditError(UUID runId, String nodeId, ErrorPayload error) {
                AuditPayload payload = AuditPayload.builder()
                                .runId(runId)
                                .nodeId(nodeId)
                                .event("ERROR_OCCURRED")
                                .level(AuditPayload.AuditLevel.ERROR)
                                .actor(AuditPayload.Actor.builder()
                                                .type(AuditPayload.ActorType.SYSTEM)
                                                .id("orchestrator")
                                                .build())
                                .metadata(java.util.Map.of(
                                                "errorType", error.getType(),
                                                "errorMessage", error.getMessage(),
                                                "retryable", error.getRetryable(),
                                                "attempt", error.getAttempt()))
                                .build();

                return auditLogger.log(payload);
        }

        /**
         * Generate cryptographic hash for audit payload.
         */
        public String generateHash(AuditPayload payload, String previousHash) {
                try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");

                        StringBuilder data = new StringBuilder();
                        data.append(payload.getTimestamp());
                        data.append(payload.getRunId());
                        data.append(payload.getNodeId() != null ? payload.getNodeId() : "");
                        data.append(payload.getEvent());
                        data.append(payload.getActor().getId());

                        if (previousHash != null) {
                                data.append(previousHash);
                        }

                        byte[] hash = digest.digest(data.toString().getBytes());
                        return HexFormat.of().formatHex(hash);

                } catch (NoSuchAlgorithmException e) {
                        log.error("Failed to generate hash", e);
                        return null;
                }
        }

        public Uni<Void> log(tech.kayys.wayang.dto.AuditEvent event) {
                // Adapt AuditEvent to AuditPayload
                AuditPayload payload = AuditPayload.builder()
                                .runId(UUID.randomUUID()) // Placeholder or derived
                                .event(event.getType())
                                .level(AuditPayload.AuditLevel.INFO)
                                .actor(AuditPayload.Actor.builder()
                                                .type(AuditPayload.ActorType.SYSTEM)
                                                .id(event.getUserId())
                                                .build())
                                .metadata(event.getMetadata())
                                .build();
                return auditLogger.log(payload);
        }
}
