package tech.kayys.wayang.billing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.model.DomainEvent;

@ApplicationScoped
public class WebhookService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebhookService.class);
    
    public Uni<Void> sendWebhook(String url, DomainEvent event) {
        LOG.info("Sending webhook to: {} event: {}", url, event.eventType());
        
        return Uni.createFrom().item(() -> {
            // HTTP POST to webhook URL
            return null;
        });
    }
}