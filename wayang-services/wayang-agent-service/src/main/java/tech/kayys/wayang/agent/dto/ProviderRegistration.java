package tech.kayys.wayang.agent.dto;

import java.util.List;

public record ProviderRegistration(
        String id,
        String name,
        ProviderType type,
        String endpoint,
        AuthType authType,
        List<String> capabilities,
        Pricing pricing) {
}
