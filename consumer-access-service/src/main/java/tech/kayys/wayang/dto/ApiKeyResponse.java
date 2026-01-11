package tech.kayys.wayang.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ApiKeyResponse(
        Long id,
        String apiKey,
        String prefix,
        String name,
        Instant createdAt,
        Instant expiresAt,
        Set<String> scopes
) {}
