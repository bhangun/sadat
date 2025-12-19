package tech.kayys.wayang.schema.governance;

public class ResourceProfile {
    private String cpu = "100m";
    private String memory = "128Mi";
    private int gpu = 0;
    private String ephemeralStorage;
    private int timeoutMs = 30000;

    // Getters and setters
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

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
