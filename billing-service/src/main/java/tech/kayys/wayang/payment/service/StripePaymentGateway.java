package tech.kayys.wayang.payment.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.payment.dto.PaymentResult;

@ApplicationScoped
public class StripePaymentGateway {
    
    public Uni<PaymentResult> createCharge(
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethodId,
            Map<String, String> metadata) {
        
        // Simulate Stripe API call
        return Uni.createFrom().item(() -> {
            String txnId = "txn_" + UUID.randomUUID().toString();
            return new PaymentResult(true, "Charge successful", txnId);
        });
    }
}