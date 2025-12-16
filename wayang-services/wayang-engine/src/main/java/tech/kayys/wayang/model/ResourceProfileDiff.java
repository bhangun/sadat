package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ResourceProfileDiff - Resource profile changes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceProfileDiff {
    public String oldCpu;
    public String newCpu;
    public String oldMemory;
    public String newMemory;
    public Integer oldGpu;
    public Integer newGpu;

    public String getDescription() {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(oldCpu, newCpu)) {
            changes.add(String.format("CPU: %s → %s", oldCpu, newCpu));
        }
        if (!Objects.equals(oldMemory, newMemory)) {
            changes.add(String.format("Memory: %s → %s", oldMemory, newMemory));
        }
        if (!Objects.equals(oldGpu, newGpu)) {
            changes.add(String.format("GPU: %d → %d", oldGpu, newGpu));
        }
        return "Resource profile changed: " + String.join(", ", changes);
    }
}
