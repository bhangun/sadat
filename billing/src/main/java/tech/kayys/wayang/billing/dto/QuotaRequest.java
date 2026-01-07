package tech.kayys.wayang.billing.dto;

 
// Request DTO (typically in separate file)
public class QuotaRequest {
    private final Integer maxUsers;
    private final Long storageGB;
    private final Integer cpuCores;
    private final Integer memoryGB;
    private final Long requestsPerMinute;
    private final Integer maxInstances;
    private final Integer publicIPs;
    private final Integer loadBalancers;
    private final Long bandwidthGB;
    private final Integer snapshotCount;
    private final Long backupStorageGB;
    private final Integer maxTeams;
    private final Integer concurrentRequests;
    
    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }
    
    private QuotaRequest(Builder builder) {
        this.maxUsers = builder.maxUsers;
        this.storageGB = builder.storageGB;
        this.cpuCores = builder.cpuCores;
        this.memoryGB = builder.memoryGB;
        this.requestsPerMinute = builder.requestsPerMinute;
        this.maxInstances = builder.maxInstances;
        this.publicIPs = builder.publicIPs;
        this.loadBalancers = builder.loadBalancers;
        this.bandwidthGB = builder.bandwidthGB;
        this.snapshotCount = builder.snapshotCount;
        this.backupStorageGB = builder.backupStorageGB;
        this.maxTeams = builder.maxTeams;
        this.concurrentRequests = builder.concurrentRequests;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public Long getStorageGB() {
        return storageGB;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public Integer getMemoryGB() {
        return memoryGB;
    }

    public Long getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public Integer getMaxInstances() {
        return maxInstances;
    }

    public Integer getPublicIPs() {
        return publicIPs;
    }

    public Integer getLoadBalancers() {
        return loadBalancers;
    }

    public Long getBandwidthGB() {
        return bandwidthGB;
    }

    public Integer getSnapshotCount() {
        return snapshotCount;
    }

    public Long getBackupStorageGB() {
        return backupStorageGB;
    }

    public Integer getMaxTeams() {
        return maxTeams;
    }

    public Integer getConcurrentRequests() {
        return concurrentRequests;
    }
    
    static class Builder {
        private Integer maxUsers;
        private Long storageGB;
        private Integer cpuCores;
        private Integer memoryGB;
        private Long requestsPerMinute;
        private Integer maxInstances;
        private Integer publicIPs;
        private Integer loadBalancers;
        private Long bandwidthGB;
        private Integer snapshotCount;
        private Long backupStorageGB;
        private Integer maxTeams;
        private Integer concurrentRequests;
        
        public Builder maxUsers(Integer maxUsers) {
            this.maxUsers = maxUsers;
            return this;
        }
        
        public Builder storageGB(Long storageGB) {
            this.storageGB = storageGB;
            return this;
        }
        
        public Builder cpuCores(Integer cpuCores) {
            this.cpuCores = cpuCores;
            return this;
        }
        
        public Builder memoryGB(Integer memoryGB) {
            this.memoryGB = memoryGB;
            return this;
        }
        
        public Builder requestsPerMinute(Long requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
            return this;
        }
        
        public Builder maxInstances(Integer maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }
        
        public Builder publicIPs(Integer publicIPs) {
            this.publicIPs = publicIPs;
            return this;
        }
        
        public Builder loadBalancers(Integer loadBalancers) {
            this.loadBalancers = loadBalancers;
            return this;
        }
        
        public Builder bandwidthGB(Long bandwidthGB) {
            this.bandwidthGB = bandwidthGB;
            return this;
        }
        
        public Builder snapshotCount(Integer snapshotCount) {
            this.snapshotCount = snapshotCount;
            return this;
        }
        
        public Builder backupStorageGB(Long backupStorageGB) {
            this.backupStorageGB = backupStorageGB;
            return this;
        }
        
        public Builder maxTeams(Integer maxTeams) {
            this.maxTeams = maxTeams;
            return this;
        }
        
        public Builder concurrentRequests(Integer concurrentRequests) {
            this.concurrentRequests = concurrentRequests;
            return this;
        }
        
        public QuotaRequest build() {
            return new QuotaRequest(this);
        }
    }
}
