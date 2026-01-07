package tech.kayys.wayang.billing.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.billing.client.FeatureFlagClient;
import tech.kayys.wayang.billing.client.ResourceQuotaClient;
import tech.kayys.wayang.billing.domain.AddonCatalog;
import tech.kayys.wayang.billing.dto.QuotaRequest;
import tech.kayys.wayang.billing.model.PlanQuotas;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.subscription.domain.SubscriptionAddon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ProvisioningService {

    private static final Logger LOG = LoggerFactory.getLogger(ProvisioningService.class);

    // In-memory cache for quota configurations (could be moved to database)
    private final Map<String, QuotaConfig> quotaConfigs = new ConcurrentHashMap<>();

    @RestClient
    ResourceQuotaClient resourceQuotaClient;

    @RestClient
    FeatureFlagClient featureFlagClient;

    /**
     * Initialize quota configurations
     */
    public ProvisioningService() {
        // Initialize default quota configurations
        initializeQuotaConfigs();
    }

    /**
     * Update resource quotas for an organization based on plan
     */
    public Uni<Void> updateResourceQuotas(Organization organization, PlanQuotas planQuotas) {
        LOG.info("Updating resource quotas for organization: {}, tenant: {}", 
                organization.id, organization.tenantId);

        return Uni.combine().all().unis(
                updateComputeQuotas(organization, planQuotas),
                updateStorageQuotas(organization, planQuotas),
                updateNetworkQuotas(organization, planQuotas),
                updateUserQuotas(organization, planQuotas),
                updateAPIRateLimits(organization, planQuotas)
        ).discardItems()
         .invoke(() -> LOG.info("Resource quotas updated successfully for tenant: {}", 
                organization.tenantId))
         .onFailure().invoke(error -> 
                LOG.error("Failed to update resource quotas for tenant: {}", 
                        organization.tenantId, error));
    }

    /**
     * Update quotas when an addon is added
     */
    public Uni<Void> updateAddonQuotas(Organization organization, SubscriptionAddon addon) {
        LOG.info("Updating addon quotas for organization: {}, addon: {}", 
                organization.id, addon.addonCatalog.name);

        AddonCatalog catalog = addon.addonCatalog;
        
        return switch (catalog.type) {
            case STORAGE -> increaseStorageQuota(organization, 
                    catalog.unitValue * addon.quantity);
            case COMPUTE -> increaseComputeQuota(organization, 
                    catalog.unitValue * addon.quantity);
            case USERS -> increaseUserQuota(organization, 
                    catalog.unitValue * addon.quantity);
            case API_CALLS -> increaseAPIRateLimit(organization, 
                    catalog.unitValue * addon.quantity);
            case SUPPORT -> updateSupportTier(organization, catalog.name);
            case CUSTOM -> updateCustomFeature(organization, catalog.featureKey);
        };
    }

    /**
     * Remove quotas when an addon is removed
     */
    public Uni<Void> removeAddonQuotas(Organization organization, SubscriptionAddon addon) {
        LOG.info("Removing addon quotas for organization: {}, addon: {}", 
                organization.id, addon.addonCatalog.name);

        AddonCatalog catalog = addon.addonCatalog;
        
        return switch (catalog.type) {
            case STORAGE -> decreaseStorageQuota(organization, 
                    catalog.unitValue * addon.quantity);
            case COMPUTE -> decreaseComputeQuota(organization, 
                    catalog.unitValue * addon.quantity);
            case USERS -> decreaseUserQuota(organization, 
                    catalog.unitValue * addon.quantity);
            case API_CALLS -> decreaseAPIRateLimit(organization, 
                    catalog.unitValue * addon.quantity);
            case SUPPORT -> downgradeSupportTier(organization);
            case CUSTOM -> removeCustomFeature(organization, catalog.featureKey);
        };
    }

    /**
     * Apply a specific quota configuration
     */
    public Uni<Void> applyQuotaConfiguration(Organization organization, String configId) {
        LOG.info("Applying quota configuration: {} for organization: {}", 
                configId, organization.id);

        QuotaConfig config = quotaConfigs.get(configId);
        if (config == null) {
            LOG.warn("Quota configuration not found: {}", configId);
            return Uni.createFrom().failure(
                    new IllegalArgumentException("Quota configuration not found: " + configId));
        }

        return resourceQuotaClient.applyQuotaConfig(organization.tenantId, config.toRequest())
                .invoke(() -> LOG.info("Quota configuration {} applied for tenant: {}", 
                        configId, organization.tenantId))
                .onFailure().invoke(error -> 
                        LOG.error("Failed to apply quota configuration {} for tenant: {}", 
                                configId, organization.tenantId, error));
    }

    /**
     * Reset quotas to plan defaults
     */
    public Uni<Void> resetToPlanDefaults(Organization organization, PlanQuotas planQuotas) {
        LOG.info("Resetting quotas to plan defaults for organization: {}", organization.id);
        
        return updateResourceQuotas(organization, planQuotas)
                .invoke(() -> LOG.info("Quotas reset to plan defaults for tenant: {}", 
                        organization.tenantId));
    }

    /**
     * Validate if organization has sufficient quota for operation
     */
    public Uni<Boolean> validateQuota(Organization organization, String quotaType, long required) {
        return resourceQuotaClient.getCurrentUsage(organization.tenantId, quotaType)
                .flatMap(currentUsage -> 
                        resourceQuotaClient.getQuotaLimit(organization.tenantId, quotaType)
                                .map(limit -> {
                                    boolean hasQuota = (currentUsage + required) <= limit;
                                    if (!hasQuota) {
                                        LOG.warn("Insufficient quota for tenant: {}, type: {}, " +
                                                "required: {}, current: {}, limit: {}",
                                                organization.tenantId, quotaType, required,
                                                currentUsage, limit);
                                    }
                                    return hasQuota;
                                })
                )
                .onFailure().recoverWithItem(false);
    }

    // Private helper methods

    private Uni<Void> updateComputeQuotas(Organization organization, PlanQuotas planQuotas) {
        return resourceQuotaClient.updateComputeQuota(organization.tenantId,
                QuotaRequest.builder()
                        .cpuCores(planQuotas.maxCpuCores())
                        .memoryGB(planQuotas.maxMemoryGB())
                        .maxInstances(planQuotas.maxInstances())
                        .build()
        );
    }

    private Uni<Void> updateStorageQuotas(Organization organization, PlanQuotas planQuotas) {
        return resourceQuotaClient.updateStorageQuota(organization.tenantId,
                QuotaRequest.builder()
                        .storageGB(planQuotas.maxStorageGB())
                        .backupStorageGB(planQuotas.maxBackupStorageGB())
                        .snapshotCount(planQuotas.maxSnapshots())
                        .build()
        );
    }

    private Uni<Void> updateNetworkQuotas(Organization organization, PlanQuotas planQuotas) {
        return resourceQuotaClient.updateNetworkQuota(organization.tenantId,
                QuotaRequest.builder()
                        .publicIPs(planQuotas.maxPublicIPs())
                        .loadBalancers(planQuotas.maxLoadBalancers())
                        .bandwidthGB(planQuotas.maxBandwidthGB())
                        .build()
        );
    }

    private Uni<Void> updateUserQuotas(Organization organization, PlanQuotas planQuotas) {
        return resourceQuotaClient.updateUserQuota(organization.tenantId,
                QuotaRequest.builder()
                        .maxUsers(planQuotas.maxUsers())
                        .maxTeams(planQuotas.maxTeams())
                        .build()
        );
    }

    private Uni<Void> updateAPIRateLimits(Organization organization, PlanQuotas planQuotas) {
        return resourceQuotaClient.updateRateLimit(organization.tenantId,
                QuotaRequest.builder()
                        .requestsPerMinute(planQuotas.apiRateLimit())
                        .concurrentRequests(planQuotas.maxConcurrentRequests())
                        .build()
        );
    }

    private Uni<Void> increaseStorageQuota(Organization organization, long additionalGB) {
        return resourceQuotaClient.increaseStorageQuota(organization.tenantId, additionalGB);
    }

    private Uni<Void> decreaseStorageQuota(Organization organization, long reduceGB) {
        return resourceQuotaClient.decreaseStorageQuota(organization.tenantId, reduceGB);
    }

    private Uni<Void> increaseComputeQuota(Organization organization, long additionalUnits) {
        return resourceQuotaClient.increaseComputeQuota(organization.tenantId, additionalUnits);
    }

    private Uni<Void> decreaseComputeQuota(Organization organization, long reduceUnits) {
        return resourceQuotaClient.decreaseComputeQuota(organization.tenantId, reduceUnits);
    }

    private Uni<Void> increaseUserQuota(Organization organization, int additionalUsers) {
        return resourceQuotaClient.increaseUserQuota(organization.tenantId, additionalUsers);
    }

    private Uni<Void> decreaseUserQuota(Organization organization, int reduceUsers) {
        return resourceQuotaClient.decreaseUserQuota(organization.tenantId, reduceUsers);
    }

    private Uni<Void> increaseAPIRateLimit(Organization organization, long additionalCalls) {
        return resourceQuotaClient.increaseRateLimit(organization.tenantId, additionalCalls);
    }

    private Uni<Void> decreaseAPIRateLimit(Organization organization, long reduceCalls) {
        return resourceQuotaClient.decreaseRateLimit(organization.tenantId, reduceCalls);
    }

    private Uni<Void> updateSupportTier(Organization organization, String supportTier) {
        return featureFlagClient.updateSupportTier(organization.tenantId, supportTier);
    }

    private Uni<Void> downgradeSupportTier(Organization organization) {
        return featureFlagClient.resetSupportTier(organization.tenantId);
    }

    private Uni<Void> updateCustomFeature(Organization organization, String featureKey) {
        return featureFlagClient.enableFeature(organization.tenantId, featureKey);
    }

    private Uni<Void> removeCustomFeature(Organization organization, String featureKey) {
        return featureFlagClient.disableFeature(organization.tenantId, featureKey);
    }

    private void initializeQuotaConfigs() {
        // Free tier
        quotaConfigs.put("free", QuotaConfig.builder()
                .name("Free Tier")
                .maxUsers(5)
                .maxStorageGB(10)
                .maxCpuCores(1)
                .maxMemoryGB(2)
                .apiRateLimit(100)
                .build());

        // Starter tier
        quotaConfigs.put("starter", QuotaConfig.builder()
                .name("Starter Tier")
                .maxUsers(20)
                .maxStorageGB(100)
                .maxCpuCores(4)
                .maxMemoryGB(8)
                .apiRateLimit(1000)
                .maxInstances(5)
                .build());

        // Professional tier
        quotaConfigs.put("professional", QuotaConfig.builder()
                .name("Professional Tier")
                .maxUsers(100)
                .maxStorageGB(500)
                .maxCpuCores(16)
                .maxMemoryGB(32)
                .apiRateLimit(10000)
                .maxInstances(20)
                .maxPublicIPs(5)
                .build());

        // Business tier
        quotaConfigs.put("business", QuotaConfig.builder()
                .name("Business Tier")
                .maxUsers(500)
                .maxStorageGB(2000)
                .maxCpuCores(64)
                .maxMemoryGB(128)
                .apiRateLimit(50000)
                .maxInstances(50)
                .maxPublicIPs(20)
                .maxLoadBalancers(3)
                .build());

        // Enterprise tier
        quotaConfigs.put("enterprise", QuotaConfig.builder()
                .name("Enterprise Tier")
                .maxUsers(Integer.MAX_VALUE) // Unlimited
                .maxStorageGB(10000)
                .maxCpuCores(256)
                .maxMemoryGB(512)
                .apiRateLimit(100000)
                .maxInstances(100)
                .maxPublicIPs(50)
                .maxLoadBalancers(10)
                .build());
    }

    // DTO classes for internal use

    private static class QuotaConfig {
        private final String name;
        private final int maxUsers;
        private final long maxStorageGB;
        private final int maxCpuCores;
        private final int maxMemoryGB;
        private final long apiRateLimit;
        private final int maxInstances;
        private final int maxPublicIPs;
        private final int maxLoadBalancers;

        static Builder builder() {
            return new Builder();
        }

        QuotaRequest toRequest() {
            return QuotaRequest.builder()
                    .maxUsers(maxUsers)
                    .storageGB(maxStorageGB)
                    .cpuCores(maxCpuCores)
                    .memoryGB(maxMemoryGB)
                    .requestsPerMinute(apiRateLimit)
                    .maxInstances(maxInstances)
                    .publicIPs(maxPublicIPs)
                    .loadBalancers(maxLoadBalancers)
                    .build();
        }

        // Builder pattern
        static class Builder {
            private String name;
            private int maxUsers = 0;
            private long maxStorageGB = 0;
            private int maxCpuCores = 0;
            private int maxMemoryGB = 0;
            private long apiRateLimit = 0;
            private int maxInstances = 0;
            private int maxPublicIPs = 0;
            private int maxLoadBalancers = 0;

            Builder name(String name) {
                this.name = name;
                return this;
            }

            Builder maxUsers(int maxUsers) {
                this.maxUsers = maxUsers;
                return this;
            }

            Builder maxStorageGB(long maxStorageGB) {
                this.maxStorageGB = maxStorageGB;
                return this;
            }

            Builder maxCpuCores(int maxCpuCores) {
                this.maxCpuCores = maxCpuCores;
                return this;
            }

            Builder maxMemoryGB(int maxMemoryGB) {
                this.maxMemoryGB = maxMemoryGB;
                return this;
            }

            Builder apiRateLimit(long apiRateLimit) {
                this.apiRateLimit = apiRateLimit;
                return this;
            }

            Builder maxInstances(int maxInstances) {
                this.maxInstances = maxInstances;
                return this;
            }

            Builder maxPublicIPs(int maxPublicIPs) {
                this.maxPublicIPs = maxPublicIPs;
                return this;
            }

            Builder maxLoadBalancers(int maxLoadBalancers) {
                this.maxLoadBalancers = maxLoadBalancers;
                return this;
            }

            QuotaConfig build() {
                return new QuotaConfig(this);
            }
        }

        private QuotaConfig(Builder builder) {
            this.name = builder.name;
            this.maxUsers = builder.maxUsers;
            this.maxStorageGB = builder.maxStorageGB;
            this.maxCpuCores = builder.maxCpuCores;
            this.maxMemoryGB = builder.maxMemoryGB;
            this.apiRateLimit = builder.apiRateLimit;
            this.maxInstances = builder.maxInstances;
            this.maxPublicIPs = builder.maxPublicIPs;
            this.maxLoadBalancers = builder.maxLoadBalancers;
        }
    }
}




