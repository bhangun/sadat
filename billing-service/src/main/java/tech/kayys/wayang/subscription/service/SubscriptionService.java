package tech.kayys.wayang.subscription.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.billing.service.AuditLogger;
import tech.kayys.wayang.billing.service.BillingService;
import tech.kayys.wayang.billing.service.NotificationService;
import tech.kayys.wayang.billing.service.ProvisioningService;
import tech.kayys.wayang.billing.service.TenantEventPublisher;
import tech.kayys.wayang.subscription.domain.Subscription;
import tech.kayys.wayang.subscription.domain.SubscriptionPlan;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.subscription.model.SubscriptionStatus;
import tech.kayys.wayang.billing.model.BillingCycle;
import tech.kayys.wayang.billing.domain.PlanQuotas;
import tech.kayys.wayang.billing.domain.ResourceQuotas;
import tech.kayys.wayang.subscription.dto.CreateSubscriptionRequest;
import tech.kayys.wayang.subscription.dto.UpdateSubscriptionRequest;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.List;

@ApplicationScoped
public class SubscriptionService {

        private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);

        @Inject
        BillingService billingService;

        @Inject
        ProvisioningService provisioningService;

        @Inject
        NotificationService notificationService;

        @Inject
        AuditLogger auditLogger;

        @Inject
        TenantEventPublisher eventPublisher;

        @Inject
        TenantEventPublisher tenantEventPublisher; // Duplicate injection name in original? Using distinct name.

        public Uni<Subscription> createSubscription(Organization organization, SubscriptionPlan plan,
                        CreateSubscriptionRequest request) {
                LOG.info("Creating subscription for organization: {}, plan: {}", organization.organizationId,
                                plan.planCode);

                // Check for existing active subscription
                return getActiveSubscriptionForOrganization(organization.organizationId)
                                .onItem().ifNotNull()
                                .failWith(() -> new IllegalStateException(
                                                "Organization already has an active subscription"))
                                .onFailure(NoSuchElementException.class).recoverWithItem((Subscription) null)
                                .flatMap(existing -> {
                                        if (existing != null) {
                                                return Uni.createFrom().failure(new IllegalStateException(
                                                                "Organization already has an active subscription"));
                                        }
                                        return performSubscriptionCreation(organization, plan, request);
                                });
        }

        private Uni<Subscription> performSubscriptionCreation(Organization organization, SubscriptionPlan plan,
                        CreateSubscriptionRequest request) {
                return Panache.withTransaction(() -> {
                        Subscription subscription = new Subscription();
                        subscription.organization = organization;
                        subscription.plan = plan;
                        subscription.status = SubscriptionStatus.ACTIVE;
                        subscription.billingCycle = request.billingCycle() != null ? request.billingCycle()
                                        : BillingCycle.MONTHLY;
                        subscription.startDate = Instant.now();
                        subscription.currentPeriodStart = Instant.now();
                        subscription.currentPeriodEnd = calculatePeriodEnd(subscription.startDate,
                                        subscription.billingCycle);
                        subscription.autoRenew = true;
                        subscription.basePrice = calculatePrice(plan, subscription.billingCycle);

                        return subscription.persist().map(v -> subscription);
                })
                                .flatMap(subscription -> provisioningService
                                                .updateResourceQuotas(organization, convertToPlanQuotas(plan.quotas))
                                                .chain(() -> provisioningService.onboardTenant(organization))
                                                .chain(() -> auditLogger.logSubscriptionCreated(subscription))
                                                .chain(() -> tenantEventPublisher
                                                                .publishSubscriptionCreated(subscription))
                                                .chain(() -> billingService.createInvoice(subscription))
                                                .map(invoice -> subscription) // Returning subscription after all chains
                                )
                                .onFailure()
                                .invoke(error -> LOG.error("Failed to process new subscription for tenant: {}",
                                                organization.tenantId, error));
        }

        public Uni<Subscription> getSubscription(UUID subscriptionId) {
                return Subscription.<Subscription>findById(subscriptionId)
                                .onItem().ifNull().failWith(() -> new NoSuchElementException("Subscription not found"));
        }

        public Uni<Subscription> getActiveSubscriptionForOrganization(UUID organizationId) {
                return Subscription.<Subscription>find("organization.organizationId = ?1 and status = ?2",
                                organizationId, SubscriptionStatus.ACTIVE)
                                .firstResult()
                                .onItem().ifNull()
                                .failWith(() -> new NoSuchElementException("No active subscription found"));
        }

        public Uni<Subscription> updateSubscription(UUID subscriptionId, UpdateSubscriptionRequest request) {
                LOG.info("Updating subscription: {}", subscriptionId);

                return Uni.combine().all()
                                .unis(
                                                Subscription.<Subscription>findById(subscriptionId),
                                                request.newPlanId() != null
                                                                ? SubscriptionPlan.<SubscriptionPlan>findById(
                                                                                request.newPlanId())
                                                                : Uni.createFrom().nullItem())
                                .asTuple()
                                .flatMap(tuple -> {
                                        Subscription sub = (Subscription) tuple.getItem1();
                                        SubscriptionPlan newPlan = (SubscriptionPlan) tuple.getItem2();

                                        if (sub == null) {
                                                return Uni.createFrom().failure(
                                                                new NoSuchElementException("Subscription not found"));
                                        }

                                        return performSubscriptionUpdate(sub, newPlan, request);
                                })
                                .flatMap(sub -> auditLogger.logSubscriptionUpdated(sub)
                                                .replaceWith(sub));
                }

        private Uni<Subscription> performSubscriptionUpdate(Subscription sub, SubscriptionPlan newPlan,
                        UpdateSubscriptionRequest request) {
                return Panache.withTransaction(() -> {
                        boolean planChanged = false;

                        if (newPlan != null && !newPlan.planId.equals(sub.plan.planId)) {
                                sub.plan = newPlan;
                                sub.basePrice = calculatePrice(newPlan, sub.billingCycle);
                                planChanged = true;
                        }

                        if (request.billingCycle() != null && request.billingCycle() != sub.billingCycle) {
                                sub.billingCycle = request.billingCycle();
                                sub.basePrice = calculatePrice(sub.plan, sub.billingCycle);
                        }

                        if (request.immediate() && planChanged) {
                                sub.currentPeriodEnd = Instant.now();
                                // sub.renewPeriod(); // Assuming this method exists or logic is here
                        }

                        sub.updatedAt = Instant.now();
                        return sub.persist().map(v -> sub);
                })
                                .flatMap(updatedSub -> {
                                        if (newPlan != null) {
                                                // updateResourceQuotas returns Uni<Void>, chain it and return
                                                // updatedSub
                                                return provisioningService
                                                                .updateResourceQuotas(updatedSub.organization,
                                                                                convertToPlanQuotas(newPlan.quotas))
                                                                .map(v -> updatedSub);
                                        }
                                        return Uni.createFrom().item(updatedSub);
                                })
                                .flatMap(updatedSub -> eventPublisher.publishSubscriptionUpdated(updatedSub)
                                                .map(v -> updatedSub))
                                .onFailure().invoke(throwable -> LOG.error("Failed to update subscription", throwable));
        }

        // Stub Helpers
        private Instant calculatePeriodEnd(Instant start, BillingCycle cycle) {
                // Simple stub logic
                if (cycle == BillingCycle.ANNUAL)
                        return start.plus(365, java.time.temporal.ChronoUnit.DAYS);
                return start.plus(30, java.time.temporal.ChronoUnit.DAYS);
        }

        private java.math.BigDecimal calculatePrice(SubscriptionPlan plan, BillingCycle cycle) {
                if (cycle == BillingCycle.ANNUAL)
                        return plan.annualPrice;
                return plan.monthlyPrice;
        }

        private Uni<Void> updateOrganizationFeatures(Organization org, SubscriptionPlan plan) {
                return Uni.createFrom().voidItem();
        }

        private PlanQuotas convertToPlanQuotas(ResourceQuotas resourceQuotas) {
                return PlanQuotas.builder()
                                .maxUsers(resourceQuotas.maxUsers)
                                .maxTeams(resourceQuotas.maxTeams)
                                .maxStorageGB((long) resourceQuotas.maxStorageGb)
                                // Map other fields as best as possible
                                .apiRateLimit(resourceQuotas.maxApiCallsPerMonth)
                                .build();
        }
}