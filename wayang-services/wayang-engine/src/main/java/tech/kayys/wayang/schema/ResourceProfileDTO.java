package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ResourceProfileDTO - Resource requirements
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceProfileDTO {

    private String cpu = "100m";
    private String memory = "128Mi";
    private int gpu = 0;
    private String ephemeralStorage;

    // Getters and setters...
    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public int getGpu() {
        return gpu;
    }

    public void setGpu(int gpu) {
        this.gpu = gpu;
    }

    public String getEphemeralStorage() {
        return ephemeralStorage;
    }

    public void setEphemeralStorage(String ephemeralStorage) {
        this.ephemeralStorage = ephemeralStorage;
    }
}