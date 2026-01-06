package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.kernel.ExecutionToken;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates execution tokens for security and replay protection
 */
@ApplicationScoped
public class ExecutionTokenValidator {

    @Inject
    ExecutionTokenRepository tokenRepository;

    @Inject
    CryptographicService cryptoService;

    private final Set<String> usedTokenSignatures = ConcurrentHashMap.newKeySet();

    public Uni<Boolean> validate(ExecutionToken token) {
        if (token == null) {
            return Uni.createFrom().item(false);
        }

        // Check expiration
        if (token.isExpired()) {
            return Uni.createFrom().item(false);
        }

        // Verify signature
        return verifySignature(token)
                .flatMap(signatureValid -> {
                    if (!signatureValid) {
                        return Uni.createFrom().item(false);
                    }

                    // Check for replay attacks
                    return checkReplayProtection(token)
                            .map(notReplayed -> notReplayed);
                });
    }

    public Uni<ExecutionToken> validateAndRefresh(ExecutionToken token) {
        return validate(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Uni.createFrom().failure(
                                new SecurityException("Invalid execution token"));
                    }

                    // Refresh token if it's about to expire
                    if (token.getExpiresAt().minusSeconds(30).isBefore(Instant.now())) {
                        return refreshToken(token);
                    }

                    return Uni.createFrom().item(token);
                });
    }

    public Uni<String> generateTokenSignature(ExecutionToken token) {
        String data = String.format("%s:%s:%s:%d:%s",
                token.getRunId(),
                token.getNodeId(),
                token.getToken(),
                token.getAttempt(),
                token.getExpiresAt().toString());

        return cryptoService.sign(data);
    }

    private Uni<Boolean> verifySignature(ExecutionToken token) {
        if (token.getSignature() == null || token.getSignature().isEmpty()) {
            return Uni.createFrom().item(false);
        }

        String data = String.format("%s:%s:%s:%d:%s",
                token.getRunId(),
                token.getNodeId(),
                token.getToken(),
                token.getAttempt(),
                token.getExpiresAt().toString());

        return cryptoService.verify(data, token.getSignature());
    }

    private Uni<Boolean> checkReplayProtection(ExecutionToken token) {
        // Check in-memory cache first (fast)
        String tokenSignature = token.getSignature();
        if (tokenSignature != null && usedTokenSignatures.contains(tokenSignature)) {
            return Uni.createFrom().item(false);
        }

        // Check persistent store
        return tokenRepository.isTokenUsed(token.getToken())
                .map(isUsed -> {
                    if (isUsed) {
                        // Also cache in memory
                        if (tokenSignature != null) {
                            usedTokenSignatures.add(tokenSignature);
                        }
                        return false;
                    }
                    return true;
                });
    }

    private Uni<ExecutionToken> refreshToken(ExecutionToken token) {
        Instant newExpiresAt = Instant.now().plusSeconds(3600); // 1 hour

        ExecutionToken refreshed = ExecutionToken.builder()
                .runId(token.getRunId())
                .nodeId(token.getNodeId())
                .token(generateNewTokenValue())
                .attempt(token.getAttempt())
                .expiresAt(newExpiresAt)
                .build();

        // Generate new signature
        return generateTokenSignature(refreshed)
                .map(signature -> {
                    refreshed.setSignature(signature);
                    return refreshed;
                })
                .flatMap(newToken -> tokenRepository.save(newToken)
                        .replaceWith(newToken));
    }

    public Uni<Void> markTokenAsUsed(ExecutionToken token) {
        return Uni.createFrom().deferred(() -> {
            // Add to in-memory cache
            if (token.getSignature() != null) {
                usedTokenSignatures.add(token.getSignature());
            }

            // Mark in persistent store
            return tokenRepository.markAsUsed(token.getToken())
                    .onFailure().invoke(th -> {
                        // Remove from cache if persistence fails
                        if (token.getSignature() != null) {
                            usedTokenSignatures.remove(token.getSignature());
                        }
                    });
        });
    }

    public Uni<Void> invalidateToken(ExecutionToken token) {
        return Uni.createFrom().deferred(() -> {
            // Remove from caches
            if (token.getSignature() != null) {
                usedTokenSignatures.remove(token.getSignature());
            }

            // Invalidate in persistent store
            return tokenRepository.invalidate(token.getToken());
        });
    }

    public Uni<Void> cleanupExpiredTokens() {
        Instant cutoff = Instant.now().minusSeconds(86400); // 24 hours ago

        return tokenRepository.deleteExpired(cutoff)
                .onItem().invoke(count -> {
                    LOG.debug("Cleaned up {} expired tokens", count);

                    // Also clean up in-memory cache (optional, as it's self-cleaning)
                    usedTokenSignatures.removeIf(sig -> {
                        // Simple heuristic - tokens older than 24 hours in cache
                        return true; // Would need timestamp tracking in cache
                    });
                });
    }

    private String generateNewTokenValue() {
        return "token-" + UUID.randomUUID() + "-" + System.currentTimeMillis();
    }
}
