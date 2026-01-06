package tech.kayys.wayang.security.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyInfo(
        UUID id,
        String keyPrefix,
        String name,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        List<String> scopes) {
}