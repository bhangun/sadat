package tech.kayys.wayang.billing.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class PlanQuotas {

    // User limits
    private Integer maxUsers;
    private Integer maxTeams;
    private Integer maxAdmins;

    // Compute resources
    private Integer maxCpuCores;
    private Integer maxMemoryGB;
    private Integer maxInstances;
    private Integer maxContainers;
    private Integer maxGpuDevices;

    // Storage
    private Long maxStorageGB;
    private Long maxBackupStorageGB;
    private Integer maxSnapshots;
    private Long maxObjectStorageGB;
    private Integer maxDatabases;

    // Network
    private Integer maxPublicIPs;
    private Integer maxLoadBalancers;
    private Long maxBandwidthGB;
    private Integer maxVpcs;
    private Integer maxSubnets;

    // API & Integration limits
    private Long apiRateLimit; // requests per minute
    private Integer maxConcurrentRequests;
    private Integer maxWebhooks;
    private Integer maxApiKeys;
    private Integer maxIntegrations;

    // Advanced features
    private Boolean allowsCustomDomains;
    private Boolean allowsSso;
    private Boolean allowsAuditLogs;
    private Boolean allowsCustomBranding;
    private Boolean allowsAdvancedAnalytics;
    private Boolean allowsAiFeatures;

    // Support
    private String supportLevel; // "basic", "priority", "enterprise"
    private Integer slaPercentage; // 99, 99.9, 99.99
    private Integer maxSupportTickets;
    private Boolean dedicatedAccountManager;

    // Data retention
    private Integer logRetentionDays;
    private Integer auditLogRetentionDays;
    private Integer backupRetentionDays;

    // Custom limits
    private Integer maxWorkflows;
    private Integer maxScheduledJobs;
    private Integer maxEventStreams;
    private Integer maxDataPipelines;

    public PlanQuotas() {
        // Default constructor for JPA
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static PlanQuotas defaultFreeTier() {
        return builder()
                .maxUsers(5)
                .maxTeams(1)
                .maxCpuCores(1)
                .maxMemoryGB(2)
                .maxStorageGB(10L)
                .apiRateLimit(100L)
                .maxConcurrentRequests(10)
                .supportLevel("basic")
                .slaPercentage(99)
                .build();
    }

    public static PlanQuotas defaultStarterTier() {
        return builder()
                .maxUsers(20)
                .maxTeams(5)
                .maxCpuCores(4)
                .maxMemoryGB(8)
                .maxStorageGB(100L)
                .maxInstances(5)
                .apiRateLimit(1000L)
                .maxConcurrentRequests(50)
                .allowsCustomDomains(true)
                .supportLevel("priority")
                .slaPercentage(99)
                .build();
    }

    public static PlanQuotas defaultProfessionalTier() {
        return builder()
                .maxUsers(100)
                .maxTeams(20)
                .maxCpuCores(16)
                .maxMemoryGB(32)
                .maxStorageGB(500L)
                .maxInstances(20)
                .maxPublicIPs(5)
                .apiRateLimit(10000L)
                .maxConcurrentRequests(200)
                .allowsCustomDomains(true)
                .allowsSso(true)
                .allowsAuditLogs(true)
                .supportLevel("priority")
                .slaPercentage(99.9f)
                .logRetentionDays(30)
                .build();
    }

    public static PlanQuotas defaultBusinessTier() {
        return builder()
                .maxUsers(500)
                .maxTeams(50)
                .maxCpuCores(64)
                .maxMemoryGB(128)
                .maxStorageGB(2000L)
                .maxInstances(50)
                .maxPublicIPs(20)
                .maxLoadBalancers(3)
                .apiRateLimit(50000L)
                .maxConcurrentRequests(500)
                .allowsCustomDomains(true)
                .allowsSso(true)
                .allowsAuditLogs(true)
                .allowsCustomBranding(true)
                .allowsAdvancedAnalytics(true)
                .supportLevel("enterprise")
                .slaPercentage(99.95f)
                .logRetentionDays(90)
                .auditLogRetentionDays(365)
                .dedicatedAccountManager(true)
                .build();
    }

    public static PlanQuotas defaultEnterpriseTier() {
        return builder()
                .maxUsers(Integer.MAX_VALUE) // Unlimited
                .maxTeams(200)
                .maxCpuCores(256)
                .maxMemoryGB(512)
                .maxStorageGB(10000L)
                .maxInstances(100)
                .maxPublicIPs(50)
                .maxLoadBalancers(10)
                .apiRateLimit(100000L)
                .maxConcurrentRequests(1000)
                .allowsCustomDomains(true)
                .allowsSso(true)
                .allowsAuditLogs(true)
                .allowsCustomBranding(true)
                .allowsAdvancedAnalytics(true)
                .allowsAiFeatures(true)
                .supportLevel("enterprise")
                .slaPercentage(99.99f)
                .logRetentionDays(365)
                .auditLogRetentionDays(730)
                .dedicatedAccountManager(true)
                .maxWorkflows(Integer.MAX_VALUE)
                .maxDataPipelines(Integer.MAX_VALUE)
                .build();
    }

    // Getters and Setters
    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxTeams() {
        return maxTeams;
    }

    public void setMaxTeams(Integer maxTeams) {
        this.maxTeams = maxTeams;
    }

    public Integer getMaxAdmins() {
        return maxAdmins;
    }

    public void setMaxAdmins(Integer maxAdmins) {
        this.maxAdmins = maxAdmins;
    }

    public Integer getMaxCpuCores() {
        return maxCpuCores;
    }

    public void setMaxCpuCores(Integer maxCpuCores) {
        this.maxCpuCores = maxCpuCores;
    }

    public Integer getMaxMemoryGB() {
        return maxMemoryGB;
    }

    public void setMaxMemoryGB(Integer maxMemoryGB) {
        this.maxMemoryGB = maxMemoryGB;
    }

    public Integer getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(Integer maxInstances) {
        this.maxInstances = maxInstances;
    }

    public Integer getMaxContainers() {
        return maxContainers;
    }

    public void setMaxContainers(Integer maxContainers) {
        this.maxContainers = maxContainers;
    }

    public Integer getMaxGpuDevices() {
        return maxGpuDevices;
    }

    public void setMaxGpuDevices(Integer maxGpuDevices) {
        this.maxGpuDevices = maxGpuDevices;
    }

    public Long getMaxStorageGB() {
        return maxStorageGB;
    }

    public void setMaxStorageGB(Long maxStorageGB) {
        this.maxStorageGB = maxStorageGB;
    }

    public Long getMaxBackupStorageGB() {
        return maxBackupStorageGB;
    }

    public void setMaxBackupStorageGB(Long maxBackupStorageGB) {
        this.maxBackupStorageGB = maxBackupStorageGB;
    }

    public Integer getMaxSnapshots() {
        return maxSnapshots;
    }

    public void setMaxSnapshots(Integer maxSnapshots) {
        this.maxSnapshots = maxSnapshots;
    }

    public Long getMaxObjectStorageGB() {
        return maxObjectStorageGB;
    }

    public void setMaxObjectStorageGB(Long maxObjectStorageGB) {
        this.maxObjectStorageGB = maxObjectStorageGB;
    }

    public Integer getMaxDatabases() {
        return maxDatabases;
    }

    public void setMaxDatabases(Integer maxDatabases) {
        this.maxDatabases = maxDatabases;
    }

    public Integer getMaxPublicIPs() {
        return maxPublicIPs;
    }

    public void setMaxPublicIPs(Integer maxPublicIPs) {
        this.maxPublicIPs = maxPublicIPs;
    }

    public Integer getMaxLoadBalancers() {
        return maxLoadBalancers;
    }

    public void setMaxLoadBalancers(Integer maxLoadBalancers) {
        this.maxLoadBalancers = maxLoadBalancers;
    }

    public Long getMaxBandwidthGB() {
        return maxBandwidthGB;
    }

    public void setMaxBandwidthGB(Long maxBandwidthGB) {
        this.maxBandwidthGB = maxBandwidthGB;
    }

    public Integer getMaxVpcs() {
        return maxVpcs;
    }

    public void setMaxVpcs(Integer maxVpcs) {
        this.maxVpcs = maxVpcs;
    }

    public Integer getMaxSubnets() {
        return maxSubnets;
    }

    public void setMaxSubnets(Integer maxSubnets) {
        this.maxSubnets = maxSubnets;
    }

    public Long getApiRateLimit() {
        return apiRateLimit;
    }

    public void setApiRateLimit(Long apiRateLimit) {
        this.apiRateLimit = apiRateLimit;
    }

    public Integer getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(Integer maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public Integer getMaxWebhooks() {
        return maxWebhooks;
    }

    public void setMaxWebhooks(Integer maxWebhooks) {
        this.maxWebhooks = maxWebhooks;
    }

    public Integer getMaxApiKeys() {
        return maxApiKeys;
    }

    public void setMaxApiKeys(Integer maxApiKeys) {
        this.maxApiKeys = maxApiKeys;
    }

    public Integer getMaxIntegrations() {
        return maxIntegrations;
    }

    public void setMaxIntegrations(Integer maxIntegrations) {
        this.maxIntegrations = maxIntegrations;
    }

    public Boolean getAllowsCustomDomains() {
        return allowsCustomDomains;
    }

    public void setAllowsCustomDomains(Boolean allowsCustomDomains) {
        this.allowsCustomDomains = allowsCustomDomains;
    }

    public Boolean getAllowsSso() {
        return allowsSso;
    }

    public void setAllowsSso(Boolean allowsSso) {
        this.allowsSso = allowsSso;
    }

    public Boolean getAllowsAuditLogs() {
        return allowsAuditLogs;
    }

    public void setAllowsAuditLogs(Boolean allowsAuditLogs) {
        this.allowsAuditLogs = allowsAuditLogs;
    }

    public Boolean getAllowsCustomBranding() {
        return allowsCustomBranding;
    }

    public void setAllowsCustomBranding(Boolean allowsCustomBranding) {
        this.allowsCustomBranding = allowsCustomBranding;
    }

    public Boolean getAllowsAdvancedAnalytics() {
        return allowsAdvancedAnalytics;
    }

    public void setAllowsAdvancedAnalytics(Boolean allowsAdvancedAnalytics) {
        this.allowsAdvancedAnalytics = allowsAdvancedAnalytics;
    }

    public Boolean getAllowsAiFeatures() {
        return allowsAiFeatures;
    }

    public void setAllowsAiFeatures(Boolean allowsAiFeatures) {
        this.allowsAiFeatures = allowsAiFeatures;
    }

    public String getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(String supportLevel) {
        this.supportLevel = supportLevel;
    }

    public Integer getSlaPercentage() {
        return slaPercentage;
    }

    public void setSlaPercentage(Integer slaPercentage) {
        this.slaPercentage = slaPercentage;
    }

    public Integer getMaxSupportTickets() {
        return maxSupportTickets;
    }

    public void setMaxSupportTickets(Integer maxSupportTickets) {
        this.maxSupportTickets = maxSupportTickets;
    }

    public Boolean getDedicatedAccountManager() {
        return dedicatedAccountManager;
    }

    public void setDedicatedAccountManager(Boolean dedicatedAccountManager) {
        this.dedicatedAccountManager = dedicatedAccountManager;
    }

    public Integer getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(Integer logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public Integer getAuditLogRetentionDays() {
        return auditLogRetentionDays;
    }

    public void setAuditLogRetentionDays(Integer auditLogRetentionDays) {
        this.auditLogRetentionDays = auditLogRetentionDays;
    }

    public Integer getBackupRetentionDays() {
        return backupRetentionDays;
    }

    public void setBackupRetentionDays(Integer backupRetentionDays) {
        this.backupRetentionDays = backupRetentionDays;
    }

    public Integer getMaxWorkflows() {
        return maxWorkflows;
    }

    public void setMaxWorkflows(Integer maxWorkflows) {
        this.maxWorkflows = maxWorkflows;
    }

    public Integer getMaxScheduledJobs() {
        return maxScheduledJobs;
    }

    public void setMaxScheduledJobs(Integer maxScheduledJobs) {
        this.maxScheduledJobs = maxScheduledJobs;
    }

    public Integer getMaxEventStreams() {
        return maxEventStreams;
    }

    public void setMaxEventStreams(Integer maxEventStreams) {
        this.maxEventStreams = maxEventStreams;
    }

    public Integer getMaxDataPipelines() {
        return maxDataPipelines;
    }

    public void setMaxDataPipelines(Integer maxDataPipelines) {
        this.maxDataPipelines = maxDataPipelines;
    }

    // Helper methods
    public boolean hasFeature(String feature) {
        return switch (feature.toLowerCase()) {
            case "custom_domains" -> Boolean.TRUE.equals(allowsCustomDomains);
            case "sso" -> Boolean.TRUE.equals(allowsSso);
            case "audit_logs" -> Boolean.TRUE.equals(allowsAuditLogs);
            case "custom_branding" -> Boolean.TRUE.equals(allowsCustomBranding);
            case "advanced_analytics" -> Boolean.TRUE.equals(allowsAdvancedAnalytics);
            case "ai_features" -> Boolean.TRUE.equals(allowsAiFeatures);
            default -> false;
        };
    }

    public boolean canAddUser(int currentUserCount) {
        return maxUsers == null || maxUsers == Integer.MAX_VALUE || currentUserCount < maxUsers;
    }

    public boolean canAddStorage(long currentStorageGB, long additionalGB) {
        return maxStorageGB == null || maxStorageGB == Long.MAX_VALUE ||
                (currentStorageGB + additionalGB) <= maxStorageGB;
    }

    public boolean canMakeApiCall(int currentRequestsThisMinute) {
        return apiRateLimit == null || apiRateLimit == Long.MAX_VALUE ||
                currentRequestsThisMinute < apiRateLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PlanQuotas that = (PlanQuotas) o;
        return Objects.equals(maxUsers, that.maxUsers) &&
                Objects.equals(maxTeams, that.maxTeams) &&
                Objects.equals(maxAdmins, that.maxAdmins) &&
                Objects.equals(maxCpuCores, that.maxCpuCores) &&
                Objects.equals(maxMemoryGB, that.maxMemoryGB) &&
                Objects.equals(maxInstances, that.maxInstances) &&
                Objects.equals(maxContainers, that.maxContainers) &&
                Objects.equals(maxGpuDevices, that.maxGpuDevices) &&
                Objects.equals(maxStorageGB, that.maxStorageGB) &&
                Objects.equals(maxBackupStorageGB, that.maxBackupStorageGB) &&
                Objects.equals(maxSnapshots, that.maxSnapshots) &&
                Objects.equals(maxObjectStorageGB, that.maxObjectStorageGB) &&
                Objects.equals(maxDatabases, that.maxDatabases) &&
                Objects.equals(maxPublicIPs, that.maxPublicIPs) &&
                Objects.equals(maxLoadBalancers, that.maxLoadBalancers) &&
                Objects.equals(maxBandwidthGB, that.maxBandwidthGB) &&
                Objects.equals(maxVpcs, that.maxVpcs) &&
                Objects.equals(maxSubnets, that.maxSubnets) &&
                Objects.equals(apiRateLimit, that.apiRateLimit) &&
                Objects.equals(maxConcurrentRequests, that.maxConcurrentRequests) &&
                Objects.equals(maxWebhooks, that.maxWebhooks) &&
                Objects.equals(maxApiKeys, that.maxApiKeys) &&
                Objects.equals(maxIntegrations, that.maxIntegrations) &&
                Objects.equals(allowsCustomDomains, that.allowsCustomDomains) &&
                Objects.equals(allowsSso, that.allowsSso) &&
                Objects.equals(allowsAuditLogs, that.allowsAuditLogs) &&
                Objects.equals(allowsCustomBranding, that.allowsCustomBranding) &&
                Objects.equals(allowsAdvancedAnalytics, that.allowsAdvancedAnalytics) &&
                Objects.equals(allowsAiFeatures, that.allowsAiFeatures) &&
                Objects.equals(supportLevel, that.supportLevel) &&
                Objects.equals(slaPercentage, that.slaPercentage) &&
                Objects.equals(maxSupportTickets, that.maxSupportTickets) &&
                Objects.equals(dedicatedAccountManager, that.dedicatedAccountManager) &&
                Objects.equals(logRetentionDays, that.logRetentionDays) &&
                Objects.equals(auditLogRetentionDays, that.auditLogRetentionDays) &&
                Objects.equals(backupRetentionDays, that.backupRetentionDays) &&
                Objects.equals(maxWorkflows, that.maxWorkflows) &&
                Objects.equals(maxScheduledJobs, that.maxScheduledJobs) &&
                Objects.equals(maxEventStreams, that.maxEventStreams) &&
                Objects.equals(maxDataPipelines, that.maxDataPipelines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                maxUsers, maxTeams, maxAdmins, maxCpuCores, maxMemoryGB, maxInstances,
                maxContainers, maxGpuDevices, maxStorageGB, maxBackupStorageGB, maxSnapshots,
                maxObjectStorageGB, maxDatabases, maxPublicIPs, maxLoadBalancers, maxBandwidthGB,
                maxVpcs, maxSubnets, apiRateLimit, maxConcurrentRequests, maxWebhooks,
                maxApiKeys, maxIntegrations, allowsCustomDomains, allowsSso, allowsAuditLogs,
                allowsCustomBranding, allowsAdvancedAnalytics, allowsAiFeatures, supportLevel,
                slaPercentage, maxSupportTickets, dedicatedAccountManager, logRetentionDays,
                auditLogRetentionDays, backupRetentionDays, maxWorkflows, maxScheduledJobs,
                maxEventStreams, maxDataPipelines);
    }

    @Override
    public String toString() {
        return "PlanQuotas{" +
                "maxUsers=" + maxUsers +
                ", maxTeams=" + maxTeams +
                ", maxCpuCores=" + maxCpuCores +
                ", maxMemoryGB=" + maxMemoryGB +
                ", maxStorageGB=" + maxStorageGB +
                ", apiRateLimit=" + apiRateLimit +
                ", supportLevel='" + supportLevel + '\'' +
                '}';
    }

    // Builder class
    public static class Builder {
        private final PlanQuotas quotas;

        public Builder() {
            this.quotas = new PlanQuotas();
        }

        public Builder maxUsers(Integer maxUsers) {
            quotas.maxUsers = maxUsers;
            return this;
        }

        public Builder maxTeams(Integer maxTeams) {
            quotas.maxTeams = maxTeams;
            return this;
        }

        public Builder maxAdmins(Integer maxAdmins) {
            quotas.maxAdmins = maxAdmins;
            return this;
        }

        public Builder maxCpuCores(Integer maxCpuCores) {
            quotas.maxCpuCores = maxCpuCores;
            return this;
        }

        public Builder maxMemoryGB(Integer maxMemoryGB) {
            quotas.maxMemoryGB = maxMemoryGB;
            return this;
        }

        public Builder maxInstances(Integer maxInstances) {
            quotas.maxInstances = maxInstances;
            return this;
        }

        public Builder maxContainers(Integer maxContainers) {
            quotas.maxContainers = maxContainers;
            return this;
        }

        public Builder maxGpuDevices(Integer maxGpuDevices) {
            quotas.maxGpuDevices = maxGpuDevices;
            return this;
        }

        public Builder maxStorageGB(Long maxStorageGB) {
            quotas.maxStorageGB = maxStorageGB;
            return this;
        }

        public Builder maxBackupStorageGB(Long maxBackupStorageGB) {
            quotas.maxBackupStorageGB = maxBackupStorageGB;
            return this;
        }

        public Builder maxSnapshots(Integer maxSnapshots) {
            quotas.maxSnapshots = maxSnapshots;
            return this;
        }

        public Builder maxObjectStorageGB(Long maxObjectStorageGB) {
            quotas.maxObjectStorageGB = maxObjectStorageGB;
            return this;
        }

        public Builder maxDatabases(Integer maxDatabases) {
            quotas.maxDatabases = maxDatabases;
            return this;
        }

        public Builder maxPublicIPs(Integer maxPublicIPs) {
            quotas.maxPublicIPs = maxPublicIPs;
            return this;
        }

        public Builder maxLoadBalancers(Integer maxLoadBalancers) {
            quotas.maxLoadBalancers = maxLoadBalancers;
            return this;
        }

        public Builder maxBandwidthGB(Long maxBandwidthGB) {
            quotas.maxBandwidthGB = maxBandwidthGB;
            return this;
        }

        public Builder maxVpcs(Integer maxVpcs) {
            quotas.maxVpcs = maxVpcs;
            return this;
        }

        public Builder maxSubnets(Integer maxSubnets) {
            quotas.maxSubnets = maxSubnets;
            return this;
        }

        public Builder apiRateLimit(Long apiRateLimit) {
            quotas.apiRateLimit = apiRateLimit;
            return this;
        }

        public Builder maxConcurrentRequests(Integer maxConcurrentRequests) {
            quotas.maxConcurrentRequests = maxConcurrentRequests;
            return this;
        }

        public Builder maxWebhooks(Integer maxWebhooks) {
            quotas.maxWebhooks = maxWebhooks;
            return this;
        }

        public Builder maxApiKeys(Integer maxApiKeys) {
            quotas.maxApiKeys = maxApiKeys;
            return this;
        }

        public Builder maxIntegrations(Integer maxIntegrations) {
            quotas.maxIntegrations = maxIntegrations;
            return this;
        }

        public Builder allowsCustomDomains(Boolean allowsCustomDomains) {
            quotas.allowsCustomDomains = allowsCustomDomains;
            return this;
        }

        public Builder allowsSso(Boolean allowsSso) {
            quotas.allowsSso = allowsSso;
            return this;
        }

        public Builder allowsAuditLogs(Boolean allowsAuditLogs) {
            quotas.allowsAuditLogs = allowsAuditLogs;
            return this;
        }

        public Builder allowsCustomBranding(Boolean allowsCustomBranding) {
            quotas.allowsCustomBranding = allowsCustomBranding;
            return this;
        }

        public Builder allowsAdvancedAnalytics(Boolean allowsAdvancedAnalytics) {
            quotas.allowsAdvancedAnalytics = allowsAdvancedAnalytics;
            return this;
        }

        public Builder allowsAiFeatures(Boolean allowsAiFeatures) {
            quotas.allowsAiFeatures = allowsAiFeatures;
            return this;
        }

        public Builder supportLevel(String supportLevel) {
            quotas.supportLevel = supportLevel;
            return this;
        }

        public Builder slaPercentage(Float slaPercentage) {
            quotas.slaPercentage = slaPercentage != null ? Math.round(slaPercentage * 100) : null;
            return this;
        }

        public Builder slaPercentage(Integer slaPercentage) {
            quotas.slaPercentage = slaPercentage;
            return this;
        }

        public Builder maxSupportTickets(Integer maxSupportTickets) {
            quotas.maxSupportTickets = maxSupportTickets;
            return this;
        }

        public Builder dedicatedAccountManager(Boolean dedicatedAccountManager) {
            quotas.dedicatedAccountManager = dedicatedAccountManager;
            return this;
        }

        public Builder logRetentionDays(Integer logRetentionDays) {
            quotas.logRetentionDays = logRetentionDays;
            return this;
        }

        public Builder auditLogRetentionDays(Integer auditLogRetentionDays) {
            quotas.auditLogRetentionDays = auditLogRetentionDays;
            return this;
        }

        public Builder backupRetentionDays(Integer backupRetentionDays) {
            quotas.backupRetentionDays = backupRetentionDays;
            return this;
        }

        public Builder maxWorkflows(Integer maxWorkflows) {
            quotas.maxWorkflows = maxWorkflows;
            return this;
        }

        public Builder maxScheduledJobs(Integer maxScheduledJobs) {
            quotas.maxScheduledJobs = maxScheduledJobs;
            return this;
        }

        public Builder maxEventStreams(Integer maxEventStreams) {
            quotas.maxEventStreams = maxEventStreams;
            return this;
        }

        public Builder maxDataPipelines(Integer maxDataPipelines) {
            quotas.maxDataPipelines = maxDataPipelines;
            return this;
        }

        public PlanQuotas build() {
            return quotas;
        }
    }
}