package tech.kayys.wayang.plugin.resource;

import java.util.Map;

/**
 * Resource Requirements
 */
public class ResourceRequirements {
    
    private String cpu;
    private String memory;
    private String ephemeralStorage;
    
    private int gpu = 0;
    
    private Map<String, String> customResources;

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

    public String getEphemeralStorage() {
        return ephemeralStorage;
    }

    public void setEphemeralStorage(String ephemeralStorage) {
        this.ephemeralStorage = ephemeralStorage;
    }

    public int getGpu() {
        return gpu;
    }

    public void setGpu(int gpu) {
        this.gpu = gpu;
    }

    public Map<String, String> getCustomResources() {
        return customResources;
    }

    public void setCustomResources(Map<String, String> customResources) {
        this.customResources = customResources;
    }

    public static class Builder {
        private String cpu;
        private String memory;
        private String ephemeralStorage;
        private int gpu;
        private Map<String, String> customResources;

        public Builder cpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        public Builder memory(String memory) {
            this.memory = memory;
            return this;
        }

        public Builder ephemeralStorage(String ephemeralStorage) {
            this.ephemeralStorage = ephemeralStorage;
            return this;
        }

        public Builder gpu(int gpu) {
            this.gpu = gpu;
            return this;
        }

        public Builder customResources(Map<String, String> customResources) {
            this.customResources = customResources;
            return this;
        }

        public ResourceRequirements build() {
            ResourceRequirements resourceRequirements = new ResourceRequirements();
            resourceRequirements.setCpu(cpu);
            resourceRequirements.setMemory(memory);
            resourceRequirements.setEphemeralStorage(ephemeralStorage);
            resourceRequirements.setGpu(gpu);
            resourceRequirements.setCustomResources(customResources);
            return resourceRequirements;
        }
    }

    public static Builder builder() {
        return new Builder();
    }


}
