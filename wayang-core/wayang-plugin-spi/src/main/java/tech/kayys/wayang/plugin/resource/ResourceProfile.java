package tech.kayys.wayang.plugin.resource;

import java.util.Map;

/**
 * Resource requirements
 */
public record ResourceProfile(
    String cpu,
    String memory,
    String gpu,
    Integer timeout,
    Map<String, String> limits
) {
    public ResourceProfile {
        limits = limits != null ? Map.copyOf(limits) : Map.of();
    }
}