package tech.kayys.wayang.security.service;

import java.util.List;

public record ApiKeyValidation(
        boolean valid,
        String tenantId,
        List<String> scopes,
        Integer rateLimit,
        String error) {
    static ApiKeyValidation valid(String tenantId, List<String> scopes, Integer rateLimit) {
        return new ApiKeyValidation(true, tenantId, scopes, rateLimit, null);
    }

    static ApiKeyValidation invalid(String error) {
        return new ApiKeyValidation(false, null, null, null, error);
    }
}