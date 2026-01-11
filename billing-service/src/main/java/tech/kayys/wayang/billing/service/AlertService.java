package tech.kayys.wayang.billing.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;

@ApplicationScoped
public class AlertService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AlertService.class);
    
    @Inject
    NotificationService notificationService;
    
    // Track alerts to avoid spam
    private final Map<String, Instant> recentAlerts = new ConcurrentHashMap<>();
    private static final Duration ALERT_COOLDOWN = Duration.ofHours(1);
    
    public Uni<Void> sendQuotaWarning(
            Organization org,
            UsageType usageType,
            double percentUsed) {
        
        String alertKey = org.tenantId + ":" + usageType + ":warning";
        
        if (shouldSendAlert(alertKey)) {
            LOG.warn("Quota warning for org: {} type: {} usage: {}%", 
                org.tenantId, usageType, percentUsed);
            
            return notificationService.sendEmail(
                org.billingEmail,
                "Quota Warning: " + usageType,
                String.format("Your usage is at %.1f%% of quota", percentUsed)
            );
        }
        
        return Uni.createFrom().voidItem();
    }
    
    public Uni<Void> sendQuotaCritical(
            Organization org,
            UsageType usageType,
            double percentUsed) {
        
        String alertKey = org.tenantId + ":" + usageType + ":critical";
        
        if (shouldSendAlert(alertKey)) {
            LOG.error("Quota critical for org: {} type: {} usage: {}%", 
                org.tenantId, usageType, percentUsed);
            
            return notificationService.sendEmail(
                org.billingEmail,
                "CRITICAL: Quota Almost Exceeded - " + usageType,
                String.format("Your usage is at %.1f%% of quota. " +
                    "Service may be throttled soon.", percentUsed)
            );
        }
        
        return Uni.createFrom().voidItem();
    }
    
    public Uni<Void> sendQuotaExceeded(
            Organization org,
            UsageType usageType,
            long currentUsage,
            long limit) {
        
        String alertKey = org.tenantId + ":" + usageType + ":exceeded";
        
        if (shouldSendAlert(alertKey)) {
            LOG.error("Quota exceeded for org: {} type: {} usage: {}/{}", 
                org.tenantId, usageType, currentUsage, limit);
            
            return notificationService.sendEmail(
                org.billingEmail,
                "Quota Exceeded: " + usageType,
                String.format("Your usage (%d) has exceeded quota limit (%d). " +
                    "Please upgrade your plan.", currentUsage, limit)
            );
        }
        
        return Uni.createFrom().voidItem();
    }
    
    private boolean shouldSendAlert(String alertKey) {
        Instant lastAlert = recentAlerts.get(alertKey);
        
        if (lastAlert == null || 
            Duration.between(lastAlert, Instant.now()).compareTo(ALERT_COOLDOWN) > 0) {
            recentAlerts.put(alertKey, Instant.now());
            return true;
        }
        
        return false;
    }
}