package tech.kayys.wayang.billing.dto;

import java.util.UUID;

import tech.kayys.wayang.billing.model.BillingCycle;

public record UpdateSubscriptionRequest(
    UUID newPlanId,
    BillingCycle billingCycle,
    boolean immediate
) {}