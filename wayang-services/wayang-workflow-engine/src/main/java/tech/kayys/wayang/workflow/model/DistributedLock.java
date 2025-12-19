package tech.kayys.wayang.workflow.model;

import java.time.Duration;

/**
 * Distributed Lock
 */
public record DistributedLock(
        String key,
        String value,
        Duration ttl) {
}