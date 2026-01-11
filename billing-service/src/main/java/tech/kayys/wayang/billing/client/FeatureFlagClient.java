package tech.kayys.wayang.billing.client;

import io.smallrye.mutiny.Uni;

public interface FeatureFlagClient {
    
    Uni<Void> updateSupportTier(String tenantId, String supportTier);
    
    Uni<Void> resetSupportTier(String tenantId);
    
    Uni<Void> enableFeature(String tenantId, String featureKey);
    
    Uni<Void> disableFeature(String tenantId, String featureKey);
}
