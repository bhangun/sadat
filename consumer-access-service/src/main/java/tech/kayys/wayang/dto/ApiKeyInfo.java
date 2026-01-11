package tech.kayys.wayang.dto;

import java.time.Instant;
import java.util.Set;

public record ApiKeyInfo(
        Long id,
        String prefix,
        String name,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        Set<String> scopes
) {}
