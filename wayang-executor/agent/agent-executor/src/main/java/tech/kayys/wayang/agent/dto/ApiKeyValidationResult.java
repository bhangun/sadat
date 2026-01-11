package tech.kayys.wayang.agent.dto;

import java.util.List;

public record ApiKeyValidationResult(
        boolean isValid,
        boolean isActive,
        String userId,
        List<String> roles,
        List<String> permissions) {
    public static ApiKeyValidationResult invalid() {
        return new ApiKeyValidationResult(false, false, null, List.of(), List.of());
    }
}