package tech.kayys.wayang.common.spi;

public record ResourceProfile(
    String cpu,
    String memory,
    int timeoutMs
) {}