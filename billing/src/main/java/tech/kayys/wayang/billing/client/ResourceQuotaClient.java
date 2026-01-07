package tech.kayys.wayang.billing.client;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.billing.dto.QuotaRequest;

public 
interface ResourceQuotaClient {
    
    Uni<Void> applyQuotaConfig(String tenantId, QuotaRequest request);
    
    Uni<Long> getCurrentUsage(String tenantId, String quotaType);
    
    Uni<Long> getQuotaLimit(String tenantId, String quotaType);
    
    Uni<Void> updateComputeQuota(String tenantId, QuotaRequest request);
    
    Uni<Void> updateStorageQuota(String tenantId, QuotaRequest request);
    
    Uni<Void> updateNetworkQuota(String tenantId, QuotaRequest request);
    
    Uni<Void> updateUserQuota(String tenantId, QuotaRequest request);
    
    Uni<Void> updateRateLimit(String tenantId, QuotaRequest request);
    
    Uni<Void> increaseStorageQuota(String tenantId, long additionalGB);
    
    Uni<Void> decreaseStorageQuota(String tenantId, long reduceGB);
    
    Uni<Void> increaseComputeQuota(String tenantId, long additionalUnits);
    
    Uni<Void> decreaseComputeQuota(String tenantId, long reduceUnits);
    
    Uni<Void> increaseUserQuota(String tenantId, int additionalUsers);
    
    Uni<Void> decreaseUserQuota(String tenantId, int reduceUsers);
    
    Uni<Void> increaseRateLimit(String tenantId, long additionalCalls);
    
    Uni<Void> decreaseRateLimit(String tenantId, long reduceCalls);
}