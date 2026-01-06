package tech.kayys.wayang.security.dto;

import java.util.List;
import java.util.Map;

public record CreateApiKeyRequest(
        String name,
        String description,
        List<String> scopes,
        Integer expiresInDays,
        Integer rateLimit,
        Map<String, Object> metadata) {
}
