package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * RetryPolicyDiff - Retry policy changes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetryPolicyDiff {
    public Integer oldMaxAttempts;
    public Integer newMaxAttempts;
    public Integer oldInitialDelayMs;
    public Integer newInitialDelayMs;
    public String oldBackoff;
    public String newBackoff;

    public String getDescription() {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(oldMaxAttempts, newMaxAttempts)) {
            changes.add(String.format("max attempts: %d → %d", oldMaxAttempts, newMaxAttempts));
        }
        if (!Objects.equals(oldInitialDelayMs, newInitialDelayMs)) {
            changes.add(String.format("initial delay: %dms → %dms", oldInitialDelayMs, newInitialDelayMs));
        }
        if (!Objects.equals(oldBackoff, newBackoff)) {
            changes.add(String.format("backoff: %s → %s", oldBackoff, newBackoff));
        }
        return "Retry policy changed: " + String.join(", ", changes);
    }
}