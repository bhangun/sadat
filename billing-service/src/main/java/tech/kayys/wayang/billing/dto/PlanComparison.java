package tech.kayys.wayang.billing.dto;

import java.util.List;

import tech.kayys.wayang.subscription.domain.SubscriptionPlan;

public record PlanComparison(
    List<SubscriptionPlan> plans
) {}
