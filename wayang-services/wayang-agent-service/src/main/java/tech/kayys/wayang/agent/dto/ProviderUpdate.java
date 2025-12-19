package tech.kayys.wayang.agent.dto;

import java.util.List;

public record ProviderUpdate(
        String endpoint,
        List<String> capabilities) {
}