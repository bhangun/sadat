package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.kernel.ExecutionToken;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for execution token persistence
 */
@ApplicationScoped
public class ExecutionTokenRepository implements PanacheRepository<ExecutionToken> {

    public Uni<ExecutionToken> findByToken(String token) {
        return find("token", token).firstResult();
    }

    public Uni<ExecutionToken> findByRunIdAndNodeId(String runId, String nodeId) {
        return find("runId = ?1 and nodeId = ?2", runId, nodeId).firstResult();
    }

    public Uni<Boolean> isTokenUsed(String token) {
        return find("token = ?1 and used = true", token)
                .firstResult()
                .map(tokenEntity -> tokenEntity != null);
    }

    public Uni<ExecutionToken> markAsUsed(String token) {
        return findByToken(token)
                .onItem().ifNotNull().transformToUni(tokenEntity -> {
                    tokenEntity.setUsed(true);
                    tokenEntity.setUsedAt(Instant.now());
                    return persist(tokenEntity);
                });
    }

    public Uni<Void> invalidate(String token) {
        return update("invalidated = true, invalidatedAt = ?1 where token = ?2",
                Instant.now(), token)
                .replaceWithVoid();
    }

    public Uni<Long> deleteExpired(Instant cutoff) {
        return delete("expiresAt < ?1", cutoff);
    }

    public Uni<Long> deleteByRunId(String runId) {
        return delete("runId", runId);
    }

    public Uni<ExecutionToken> createToken(String runId, String nodeId, int attempt,
            Instant expiresAt) {
        ExecutionToken token = ExecutionToken.builder()
                .token(generateTokenValue())
                .runId(runId)
                .nodeId(nodeId)
                .attempt(attempt)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .used(false)
                .invalidated(false)
                .build();

        return persist(token);
    }

    public Uni<ExecutionToken> refreshToken(String oldToken, Instant newExpiresAt) {
        return findByToken(oldToken)
                .onItem().ifNotNull().transformToUni(existing -> {
                    ExecutionToken refreshed = ExecutionToken.builder()
                            .token(generateTokenValue())
                            .runId(existing.getRunId())
                            .nodeId(existing.getNodeId())
                            .attempt(existing.getAttempt())
                            .expiresAt(newExpiresAt)
                            .createdAt(Instant.now())
                            .used(false)
                            .invalidated(false)
                            .build();

                    // Invalidate old token
                    existing.setInvalidated(true);
                    existing.setInvalidatedAt(Instant.now());

                    return persist(existing)
                            .flatMap(ignored -> persist(refreshed));
                });
    }

    public Uni<Boolean> validateToken(String token, String runId, String nodeId) {
        return find("token = ?1 and runId = ?2 and nodeId = ?3 and used = false " +
                "and invalidated = false and expiresAt > ?4",
                token, runId, nodeId, Instant.now())
                .firstResult()
                .map(tokenEntity -> tokenEntity != null);
    }

    public Uni<Long> cleanupExpiredTokens() {
        Instant now = Instant.now();
        return delete("expiresAt < ?1 or (invalidated = true and invalidatedAt < ?2)",
                now, now.minusSeconds(86400)); // 24 hours
    }

    private String generateTokenValue() {
        return "token-" + UUID.randomUUID() + "-" + System.currentTimeMillis();
    }
}