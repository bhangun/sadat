package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logger for audit events.
 * In production, this should write to a dedicated audit database or stream.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    // In-memory storage for demonstration
    // In production, use PostgreSQL audit table or event stream
    private final Map<String, AuditPayload> auditLog = new ConcurrentHashMap<>();
    private String lastHash = null;

    /**
     * Log an audit event.
     */
    public Uni<Void> log(AuditPayload payload) {
        return Uni.createFrom().item(() -> {
            // Generate hash with chain
            String hash = generateHash(payload);
            payload.setHash(hash);
            payload.setPreviousHash(lastHash);
            lastHash = hash;

            // Store
            String key = payload.getRunId() + ":" + payload.getTimestamp();
            auditLog.put(key, payload);

            // Also log to file/console
            log.info("AUDIT: event={}, runId={}, nodeId={}, actor={}",
                    payload.getEvent(),
                    payload.getRunId(),
                    payload.getNodeId(),
                    payload.getActor().getId());

            return null;
        });
    }

    /**
     * Generate hash for audit payload.
     */
    private String generateHash(AuditPayload payload) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");

            StringBuilder data = new StringBuilder();
            data.append(payload.getTimestamp());
            data.append(payload.getRunId());
            data.append(payload.getEvent());
            if (lastHash != null) {
                data.append(lastHash);
            }

            byte[] hash = digest.digest(data.toString().getBytes());
            return java.util.HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            log.error("Failed to generate hash", e);
            return java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * Get audit trail for a run.
     */
    public Uni<java.util.List<AuditPayload>> getAuditTrail(java.util.UUID runId) {
        return Uni.createFrom().item(() -> auditLog.values().stream()
                .filter(payload -> payload.getRunId().equals(runId))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .toList());
    }
}