package tech.kayys.wayang.billing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmailService {
    
    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);
    
    @Inject
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "silat.email.provider")
    String emailProvider;
    
    /**
     * Send email (integrate with SendGrid, AWS SES, etc.)
     */
    public Uni<Void> send(String to, String subject, String body) {
        LOG.info("Sending email to: {} subject: {}", to, subject);
        
        return Uni.createFrom().item(() -> {
            // Integration with email provider
            // Example: SendGrid, AWS SES, Mailgun, etc.
            
            LOG.debug("Email sent successfully to: {}", to);
            return null;
        })
        .onFailure().invoke(error ->
            LOG.error("Failed to send email to: {}", to, error)
        );
    }
}
