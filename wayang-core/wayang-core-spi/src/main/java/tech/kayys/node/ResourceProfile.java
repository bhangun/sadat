package tech.kayys.node;

import java.time.Duration;

public class ResourceProfile {
    String cpu;           // e.g., "500m"
    String memory;        // e.g., "256Mi"
    boolean requiresGpu;
    Integer gpuCount;
    Duration timeout;
    Integer maxRetries;
}