package tech.kayys.wayang.billing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.organization.domain.Organization;

@ApplicationScoped
public class SlackService {
    
    private static final Logger LOG = LoggerFactory.getLogger(SlackService.class);
    
    public Uni<Void> sendAlert(Organization org, String title, String message) {
        // Check if Slack webhook is configured
        String webhookUrl = (String) org.metadata.get("slackWebhook");
        if (webhookUrl == null) {
            return Uni.createFrom().voidItem();
        }
        
        LOG.info("Sending Slack alert: {} to org: {}", title, org.tenantId);
        
        return Uni.createFrom().item(() -> {
            // Send to Slack webhook
            return null;
        });
    }
}

