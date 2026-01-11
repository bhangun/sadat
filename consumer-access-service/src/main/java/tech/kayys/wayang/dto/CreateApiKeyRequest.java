package tech.kayys.wayang.dto;

import java.util.Map;
import java.util.Set;

public record CreateApiKeyRequest(
        String name,
        String description,
        Integer expiresInDays,
        Set<String> scopes,
        Integer rateLimit,
        Map<String, String> metadata
) {}
