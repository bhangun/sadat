package tech.kayys.wayang.subscription.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import tech.kayys.wayang.billing.model.BillingCycle;

public record CreateSubscriptionRequest(
    @NotNull UUID organizationId,
    @NotNull UUID planId,
    BillingCycle billingCycle,
    String paymentMethodId,
    boolean startTrial
) {}
