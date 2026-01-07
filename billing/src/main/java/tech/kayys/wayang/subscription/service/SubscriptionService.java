package tech.kayys.wayang.subscription.service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Flow.Subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.billing.domain.AddonCatalog;
import tech.kayys.wayang.billing.dto.AddAddonRequest;
import tech.kayys.wayang.billing.dto.UpdateSubscriptionRequest;
import tech.kayys.wayang.billing.model.BillingCycle;
import tech.kayys.wayang.billing.model.PlanTier;
import tech.kayys.wayang.billing.service.AuditLogger;
import tech.kayys.wayang.billing.service.NotificationService;
import tech.kayys.wayang.billing.service.ProvisioningService;
import tech.kayys.wayang.billing.service.TenantEventPublisher;
import tech.kayys.wayang.organization.domain.Organization;

import tech.kayys.wayang.subscription.domain.SubscriptionAddon;
import tech.kayys.wayang.subscription.domain.SubscriptionPlan;
import tech.kayys.wayang.subscription.dto.CreateSubscriptionRequest;
import tech.kayys.wayang.subscription.model.SubscriptionStatus;

@ApplicationScoped
public class SubscriptionService {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);

    @Inject
    ProvisioningService provisioningService;

    @Inject
    TenantEventPublisher eventPublisher;

    @Inject
    AuditLogger auditLogger;

    @Inject
    NotificationService notificationService;

    /**
     * Create subscription for organization
     */
    public Uni<Subscription> createSubscription(CreateSubscriptionRequest request) {
        LOG.info("Creating subscription for org: {}", request.organizationId());

        return Uni.combine().all()
                .unis(
                        Organization.<Organization>findById(request.organizationId()),
                        SubscriptionPlan.<SubscriptionPlan>findById(request.planId()))
                .asTuple()
                .flatMap(tuple -> {
                    Organization org = tuple.getItem1();
                    SubscriptionPlan plan = tuple.getItem2();

                    if (org == null) {
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Organization not found"));
                    }
                    if (plan == null) {
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Plan not found"));
                    }
                    if (!plan.isActive) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Plan is not active"));
                    }

                    return createSubscriptionForOrganization(org, plan, request);
                })
                .invoke(sub -> LOG.info("Subscription created: {} for org: {}",
                        sub.subscriptionId, sub.organization.tenantId));
    }

    private Uni<Subscription> createSubscriptionForOrganization(
            Organization org,
            SubscriptionPlan plan,
            CreateSubscriptionRequest request) {

        return Panache.withTransaction(() -> {
                    Subscription sub = new Subscription();
                    sub.organization = org;
                    sub.plan = plan;
                    sub.status = SubscriptionStatus.ACTIVE;
                    sub.billingCycle = request.billingCycle() != null ? request.billingCycle() : BillingCycle.MONTHLY;
                    sub.createdAt = Instant.now();
                    sub.updatedAt = Instant.now();
                    sub.activatedAt = Instant.now();

                    // Set pricing based on billing cycle
                    sub.currency = plan.currency;
                    sub.basePrice = calculatePrice(plan, sub.billingCycle);

                    // Set billing period
                    sub.currentPeriodStart = Instant.now();
                    sub.currentPeriodEnd = calculatePeriodEnd(
                            sub.currentPeriodStart,
                            sub.billingCycle);

                    // Handle trial
                    if (request.startTrial() && plan.allowsTrial) {
                        sub.isTrial = true;
                        sub.trialStart = Instant.now();
                        sub.trialEnd = Instant.now().plus(Duration.ofDays(plan.trialDays));
                    }

                    // Set payment method
                    sub.paymentMethodId = request.paymentMethodId();

                    return sub.persist()
                            .map(v -> sub);
                })
                .flatMap(sub -> {
                    // Set as active subscription
                    return Panache.withTransaction(() -> {
                        sub.organization.activeSubscription = sub;
                        return sub.organization.persist()
                                .replaceWith(sub);
                    });
                })
                .flatMap(sub -> {
                    // Update organization feature flags based on plan
                    return updateOrganizationFeatures(sub.organization, plan)
                            .replaceWith(sub);
                })
                .flatMap(sub -> {
                    // Update resource quotas
                    return provisioningService.updateResourceQuotas(
                            sub.organization,
                            plan.quotas).replaceWith(sub);
                })
                .flatMap(sub -> eventPublisher.publishSubscriptionCreated(sub)
                        .replaceWith(sub))
                .flatMap(sub -> notificationService.sendSubscriptionConfirmation(sub)
                        .replaceWith(sub))
                .invoke(sub -> auditLogger.logSubscriptionCreated(sub))
                .onFailure().invoke(throwable -> LOG.error("Failed to create subscription", throwable));
    }

    /**
     * Get subscription by ID
     */
    public Uni<Subscription> getSubscription(UUID subscriptionId) {
        return Subscription.<Subscription>findById(subscriptionId)
                .onItem().ifNull().failWith(() -> new NoSuchElementException("Subscription not found"));
    }

    /**
     * Get active subscription for organization
     */
    public Uni<Subscription> getActiveSubscriptionForOrganization(UUID organizationId) {
        return Subscription.<Subscription>find("organization.id = ?1 and status = ?2",
                        organizationId, SubscriptionStatus.ACTIVE)
                .firstResult()
                .onItem().ifNull().failWith(() -> new NoSuchElementException("No active subscription found"));
    }

    /**
     * Update subscription (upgrade/downgrade)
     */
    public Uni<Subscription> updateSubscription(
            UUID subscriptionId,
            UpdateSubscriptionRequest request) {

        LOG.info("Updating subscription: {}", subscriptionId);

        return Uni.combine().all()
                .unis(
                        Subscription.<Subscription>findById(subscriptionId),
                        request.newPlanId() != null ? SubscriptionPlan.<SubscriptionPlan>findById(request.newPlanId())
                                : Uni.createFrom().nullItem())
                .asTuple()
                .flatMap(tuple -> {
                    Subscription sub = tuple.getItem1();
                    SubscriptionPlan newPlan = tuple.getItem2();

                    if (sub == null) {
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Subscription not found"));
                    }

                    return performSubscriptionUpdate(sub, newPlan, request);
                })
                .invoke(sub -> auditLogger.logSubscriptionUpdated(sub));
    }

    private Uni<Subscription> performSubscriptionUpdate(
            Subscription sub,
            SubscriptionPlan newPlan,
            UpdateSubscriptionRequest request) {

        return Panache.withTransaction(() -> {
                    boolean planChanged = false;

                    // Change plan
                    if (newPlan != null && !newPlan.planId.equals(sub.plan.planId)) {
                        SubscriptionPlan oldPlan = sub.plan;
                        sub.plan = newPlan;
                        sub.basePrice = calculatePrice(newPlan, sub.billingCycle);
                        planChanged = true;

                        LOG.info("Plan changed from {} to {}",
                                oldPlan.name, newPlan.name);
                    }

                    // Change billing cycle
                    if (request.billingCycle() != null &&
                            request.billingCycle() != sub.billingCycle) {
                        sub.billingCycle = request.billingCycle();
                        sub.basePrice = calculatePrice(sub.plan, sub.billingCycle);
                    }

                    // Handle immediate vs end-of-period change
                    if (request.immediate() && planChanged) {
                        // Prorate current period
                        sub.currentPeriodEnd = Instant.now();
                        sub.renewPeriod();
                    }

                    sub.updatedAt = Instant.now();

                    return sub.persist()
                            .map(v -> sub);
                })
                .flatMap(updatedSub -> {
                    if (newPlan != null) {
                        // Update organization features
                        return updateOrganizationFeatures(updatedSub.organization, newPlan)
                                .flatMap(v -> provisioningService.updateResourceQuotas(
                                        updatedSub.organization,
                                        newPlan.quotas))
                                .replaceWith(updatedSub);
                    }
                    return Uni.createFrom().item(updatedSub);
                })
                .flatMap(updatedSub -> eventPublisher.publishSubscriptionUpdated(updatedSub)
                        .replaceWith(updatedSub))
                .onFailure().invoke(throwable -> LOG.error("Failed to update subscription", throwable));
    }

    /**
     * Cancel subscription
     */
    public Uni<Subscription> cancelSubscription(
            UUID subscriptionId,
            boolean immediate,
            String reason) {

        LOG.warn("Cancelling subscription: {} immediate: {}",
                subscriptionId, immediate);

        return Panache.withTransaction(() -> Subscription.<Subscription>findById(subscriptionId)
                        .flatMap(sub -> {
                            if (sub == null) {
                                return Uni.createFrom().failure(
                                        new NoSuchElementException("Subscription not found"));
                            }

                            if (immediate) {
                                sub.status = SubscriptionStatus.CANCELLED;
                                sub.currentPeriodEnd = Instant.now();
                            } else {
                                sub.cancelAtPeriodEnd = true;
                            }

                            sub.cancel(reason);

                            return sub.persist()
                                    .map(v -> sub);
                        }))
                .flatMap(sub -> eventPublisher.publishSubscriptionCancelled(sub, immediate)
                        .replaceWith(sub))
                .flatMap(sub -> notificationService.sendCancellationConfirmation(sub)
                        .replaceWith(sub))
                .invoke(sub -> auditLogger.logSubscriptionCancelled(sub, reason))
                .onFailure().invoke(throwable -> LOG.error("Failed to cancel subscription", throwable));
    }

    /**
     * Add addon to subscription
     */
    public Uni<SubscriptionAddon> addAddon(
            UUID subscriptionId,
            AddAddonRequest request) {

        LOG.info("Adding addon to subscription: {}", subscriptionId);

        return Uni.combine().all()
                .unis(
                        Subscription.<Subscription>findById(subscriptionId),
                        AddonCatalog.<AddonCatalog>findById(request.addonCatalogId()))
                .asTuple()
                .flatMap(tuple -> {
                    Subscription sub = tuple.getItem1();
                    AddonCatalog catalog = tuple.getItem2();

                    if (sub == null || catalog == null) {
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Subscription or addon not found"));
                    }

                    return Panache.withTransaction(() -> {
                        SubscriptionAddon addon = new SubscriptionAddon();
                        addon.subscription = sub;
                        addon.addonCatalog = catalog;
                        addon.quantity = request.quantity();
                        addon.price = catalog.unitPrice.multiply(
                                BigDecimal.valueOf(request.quantity()));
                        addon.isActive = true;
                        addon.addedAt = Instant.now();

                        return addon.persist()
                                .map(v -> addon);
                    });
                })
                .flatMap(addon -> {
                    // Update quotas when addon is added
                    return provisioningService.updateAddonQuotas(addon.subscription.organization, addon)
                            .replaceWith(addon);
                })
                .invoke(addon -> LOG.info("Addon added: {} to subscription: {}",
                        addon.addonCatalog.name, subscriptionId))
                .onFailure().invoke(throwable -> LOG.error("Failed to add addon", throwable));
    }

    /**
     * Remove addon
     */
    public Uni<Void> removeAddon(UUID subscriptionId, UUID addonId) {
        return Panache.withTransaction(() -> SubscriptionAddon.<SubscriptionAddon>findById(addonId)
                        .flatMap(addon -> {
                            if (addon == null ||
                                    !addon.subscription.subscriptionId.equals(subscriptionId)) {
                                return Uni.createFrom().failure(
                                        new NoSuchElementException("Addon not found"));
                            }

                            addon.isActive = false;
                            addon.removedAt = Instant.now();
                            return addon.persist()
                                    .replaceWithVoid();
                        }))
                .flatMap(v -> {
                    // Update quotas when addon is removed
                    return SubscriptionAddon.<SubscriptionAddon>findById(addonId)
                            .flatMap(addon -> provisioningService.removeAddonQuotas(
                                    addon.subscription.organization, addon))
                            .replaceWithVoid();
                })
                .invoke(() -> LOG.info("Addon removed: {} from subscription: {}",
                        addonId, subscriptionId))
                .onFailure().invoke(throwable -> LOG.error("Failed to remove addon", throwable));
    }

    /**
     * Process subscription renewals (scheduled job)
     */
    @Scheduled(every = "1h")
    public Uni<Void> processRenewals() {
        LOG.info("Processing subscription renewals");

        Instant now = Instant.now();
        Instant renewalWindow = now.plus(Duration.ofHours(24));

        return Subscription.<Subscription>find(
                        "status = ?1 and currentPeriodEnd <= ?2 and currentPeriodEnd > ?3",
                        SubscriptionStatus.ACTIVE,
                        renewalWindow,
                        now)
                .list()
                .flatMap(subscriptions -> {
                    LOG.info("Found {} subscriptions to renew", subscriptions.size());
                    return Uni.combine().all().unis(
                                    subscriptions.stream()
                                            .map(this::renewSubscription)
                                            .toList()
                            )
                            .discardItems();
                })
                .invoke(() -> LOG.info("Completed processing renewals"))
                .onFailure().invoke(error -> LOG.error("Error processing renewals", error));
    }

    private Uni<Void> renewSubscription(Subscription subscription) {
        LOG.info("Renewing subscription: {}", subscription.subscriptionId);

        // Check if cancelled
        if (subscription.cancelAtPeriodEnd) {
            subscription.status = SubscriptionStatus.CANCELLED;
            return Panache.withTransaction(() -> subscription.persist())
                    .invoke(v -> LOG.info("Subscription cancelled at period end: {}",
                            subscription.subscriptionId))
                    .onFailure().invoke(error -> LOG.error("Error cancelling subscription", error))
                    .replaceWithVoid();
        }

        // Renew period
        return Panache.withTransaction(() -> {
                    subscription.renewPeriod();
                    return subscription.persist();
                })
                .invoke(v -> LOG.info("Subscription renewed: {}", subscription.subscriptionId))
                .flatMap(v -> eventPublisher.publishSubscriptionRenewed(subscription))
                .onFailure().invoke(error -> LOG.error("Error renewing subscription", error))
                .replaceWithVoid();
    }

    /**
     * Process expired trials
     */
    @Scheduled(every = "6h")
    public Uni<Void> processExpiredTrials() {
        LOG.info("Processing expired trials");

        Instant now = Instant.now();

        return Subscription.<Subscription>find(
                        "status = ?1 and isTrial = ?2 and trialEnd <= ?3",
                        SubscriptionStatus.ACTIVE,
                        true,
                        now)
                .list()
                .flatMap(subscriptions -> {
                    LOG.info("Found {} expired trials", subscriptions.size());
                    return Uni.combine().all().unis(
                                    subscriptions.stream()
                                            .map(this::endTrial)
                                            .toList()
                            )
                            .discardItems();
                })
                .invoke(() -> LOG.info("Completed processing expired trials"))
                .onFailure().invoke(error -> LOG.error("Error processing expired trials", error));
    }

    private Uni<Void> endTrial(Subscription subscription) {
        LOG.info("Ending trial for subscription: {}", subscription.subscriptionId);
        
        return Panache.withTransaction(() -> {
                    subscription.isTrial = false;
                    subscription.trialEnd = null;
                    subscription.updatedAt = Instant.now();
                    return subscription.persist();
                })
                .flatMap(v -> notificationService.sendTrialEndedNotification(subscription))
                .invoke(v -> LOG.info("Trial ended for subscription: {}", subscription.subscriptionId))
                .onFailure().invoke(error -> LOG.error("Error ending trial", error))
                .replaceWithVoid();
    }

    // Helper methods

    private BigDecimal calculatePrice(SubscriptionPlan plan, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> plan.monthlyPrice;
            case QUARTERLY -> plan.monthlyPrice.multiply(BigDecimal.valueOf(3))
                    .multiply(BigDecimal.valueOf(0.95)); // 5% discount
            case ANNUAL -> plan.annualPrice != null ? plan.annualPrice
                    : plan.monthlyPrice.multiply(BigDecimal.valueOf(12))
                    .multiply(BigDecimal.valueOf(0.85)); // 15% discount
        };
    }

    private Instant calculatePeriodEnd(Instant start, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> start.plus(30, ChronoUnit.DAYS);
            case QUARTERLY -> start.plus(90, ChronoUnit.DAYS);
            case ANNUAL -> start.plus(365, ChronoUnit.DAYS);
        };
    }

    private Uni<Void> updateOrganizationFeatures(
            Organization org,
            SubscriptionPlan plan) {

        return Panache.withTransaction(() -> {
            org.featureFlags = initializeFeatureFlags(plan.tier);
            return org.persist()
                    .replaceWithVoid();
        });
    }

    private Map<String, Boolean> initializeFeatureFlags(PlanTier tier) {
        Map<String, Boolean> flags = new HashMap<>();
        flags.put("workflow_engine", true);
        flags.put("control_plane", tier.ordinal() >= PlanTier.STARTER.ordinal());
        flags.put("ai_agents", tier.ordinal() >= PlanTier.PROFESSIONAL.ordinal());
        flags.put("advanced_integrations", tier.ordinal() >= PlanTier.BUSINESS.ordinal());
        flags.put("sla_guarantee", tier.ordinal() >= PlanTier.ENTERPRISE.ordinal());
        flags.put("dedicated_support", tier.ordinal() >= PlanTier.ENTERPRISE.ordinal());
        flags.put("custom_branding", tier.ordinal() >= PlanTier.BUSINESS.ordinal());
        flags.put("audit_logs", tier.ordinal() >= PlanTier.PROFESSIONAL.ordinal());
        flags.put("sso", tier.ordinal() >= PlanTier.BUSINESS.ordinal());
        return flags;
    }
}