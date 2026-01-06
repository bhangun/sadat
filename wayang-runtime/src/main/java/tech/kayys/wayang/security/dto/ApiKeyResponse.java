package tech.kayys.wayang.security.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String key, // Only shown once!
        String keyPrefix,
        String name,
        Instant createdAt,
        Instant expiresAt,
        List<String> scopes) {
}