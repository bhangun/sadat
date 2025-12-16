package tech.kayys.wayang.plugin.resource;

import java.util.Map;

/**
 * Resource Usage
 */
public class ResourceUsage {
    
    private double cpuUsagePercent;
    private double memoryUsageMb;
    private double diskUsageMb;
    private int gpuCount;
    
    private Map<String, Object> customMetrics;

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public double getMemoryUsageMb() {
        return memoryUsageMb;
    }

    public void setMemoryUsageMb(double memoryUsageMb) {
        this.memoryUsageMb = memoryUsageMb;
    }

    public double getDiskUsageMb() {
        return diskUsageMb;
    }

    public void setDiskUsageMb(double diskUsageMb) {
        this.diskUsageMb = diskUsageMb;
    }

    public int getGpuCount() {
        return gpuCount;
    }

    public void setGpuCount(int gpuCount) {
        this.gpuCount = gpuCount;
    }

    public Map<String, Object> getCustomMetrics() {
        return customMetrics;
    }

    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = customMetrics;
    }

       public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double cpuUsagePercent;
        private double memoryUsageMb;
        private double diskUsageMb;
        private int gpuCount;
        private Map<String, Object> customMetrics;

        public Builder cpuUsagePercent(double cpuUsagePercent) {
            this.cpuUsagePercent = cpuUsagePercent;
            return this;
        }

        public Builder memoryUsageMb(double memoryUsageMb) {
            this.memoryUsageMb = memoryUsageMb;
            return this;
        }

        public Builder diskUsageMb(double diskUsageMb) {
            this.diskUsageMb = diskUsageMb;
            return this;
        }

        public Builder gpuCount(int gpuCount) {
            this.gpuCount = gpuCount;
            return this;
        }

        public Builder customMetrics(Map<String, Object> customMetrics) {
            this.customMetrics = customMetrics;
            return this;
        }

        public ResourceUsage build() {
            ResourceUsage resourceUsage = new ResourceUsage();
            resourceUsage.setCpuUsagePercent(cpuUsagePercent);
            resourceUsage.setMemoryUsageMb(memoryUsageMb);
            resourceUsage.setDiskUsageMb(diskUsageMb);
            resourceUsage.setGpuCount(gpuCount);
            resourceUsage.setCustomMetrics(customMetrics);
            return resourceUsage;
        }
    }
}