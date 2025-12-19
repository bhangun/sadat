package tech.kayys.wayang.common.spi;

public record TelemetryConfig(
    boolean enabled,
    double sampleRate
) {}