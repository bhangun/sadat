package tech.kayys.wayang.payment.service;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.payment.dto.PaymentResult;

/**
 * Payment processor
 */
@ApplicationScoped
public class PaymentProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(PaymentProcessor.class);
    
    @Inject
    StripePaymentGateway stripeGateway;
    
    public Uni<PaymentResult> charge(
            Organization org,
            BigDecimal amount,
            String currency,
            String paymentMethodId,
            Map<String, String> metadata) {
        
        LOG.info("Processing charge: {} {} for {}", amount, currency, org.tenantId);
        
        return stripeGateway.createCharge(
            org.tenantId,
            amount,
            currency,
            paymentMethodId,
            metadata
        );
    }
}
