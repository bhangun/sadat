package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.client.MLModelClient;
import tech.kayys.wayang.billing.domain.ChurnPrediction;
import tech.kayys.wayang.billing.model.ChurnFeatures;
import tech.kayys.wayang.billing.model.MLPredictionResponse;
import tech.kayys.wayang.billing.model.PlanTier;
import tech.kayys.wayang.billing.model.RetentionStrategy;
import tech.kayys.wayang.billing.model.RiskFactor;
import tech.kayys.wayang.billing.model.RiskLevel;

/**
 * ML Model service (integrates with TensorFlow/PyTorch)
 */
@ApplicationScoped
public class MLModelService {

    private static final Logger LOG = LoggerFactory.getLogger(MLModelService.class);

    @Inject
    MLModelClient modelClient;

    /**
     * Predict churn using ML model
     */
    public Uni<ChurnPrediction> predictChurn(ChurnFeatures features) {
        LOG.debug("Calling ML model for prediction");

        return modelClient.predict(features.toVector())
                .map(response -> {
                    ChurnPrediction prediction = new ChurnPrediction();
                    prediction.churnProbability = response.probability;
                    prediction.confidenceScore = response.confidence;
                    prediction.riskLevel = determineRiskLevel(response.probability);
                    prediction.modelVersion = response.modelVersion;

                    // Feature importance from model
                    prediction.featureImportance = response.featureImportance;

                    // Analyze risk factors
                    prediction.riskFactors = analyzeRiskFactors(features, response);

                    // Generate retention strategies
                    prediction.retentionStrategies = generateRetentionStrategies(
                            features,
                            prediction.riskFactors);

                    // Predict churn date
                    if (response.probability > 0.5) {
                        int daysUntilChurn = (int) (90 * (1 - response.probability));
                        prediction.predictedChurnDate = Instant.now()
                                .plus(daysUntilChurn, ChronoUnit.DAYS);
                    }

                    return prediction;
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.error("ML model prediction failed, using fallback", error);
                    return fallbackPrediction(features);
                });
    }

    /**
     * Fallback prediction using rules
     */
    private ChurnPrediction fallbackPrediction(ChurnFeatures features) {
        ChurnPrediction prediction = new ChurnPrediction();

        // Simple rule-based prediction
        double score = 0.0;

        if (features.engagementScore < 20)
            score += 0.3;
        if (features.usageTrend < -0.5)
            score += 0.2;
        if (features.billingHealth < 50)
            score += 0.2;
        if (features.hasFailedPayments)
            score += 0.2;
        if (features.tenureMonths < 3)
            score += 0.1;

        prediction.churnProbability = Math.min(1.0, score);
        prediction.confidenceScore = 0.6; // Lower confidence for fallback
        prediction.riskLevel = determineRiskLevel(prediction.churnProbability);
        prediction.modelVersion = "fallback-v1";
        prediction.riskFactors = new ArrayList<>();
        prediction.retentionStrategies = new ArrayList<>();

        return prediction;
    }

    private RiskLevel determineRiskLevel(double probability) {
        if (probability < 0.2)
            return RiskLevel.LOW;
        if (probability < 0.5)
            return RiskLevel.MEDIUM;
        if (probability < 0.8)
            return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    private List<RiskFactor> analyzeRiskFactors(
            ChurnFeatures features,
            MLPredictionResponse response) {

        List<RiskFactor> factors = new ArrayList<>();

        // Analyze based on feature importance
        if (response.featureImportance.getOrDefault("engagement", 0.0) > 0.1 &&
                features.engagementScore < 30) {
            factors.add(new RiskFactor(
                    "Low Engagement",
                    -0.8,
                    "User engagement is significantly below average"));
        }

        if (response.featureImportance.getOrDefault("usageTrend", 0.0) > 0.1 &&
                features.usageTrend < -0.3) {
            factors.add(new RiskFactor(
                    "Declining Usage",
                    -0.7,
                    "Usage has declined by " +
                            String.format("%.0f%%", Math.abs(features.usageTrend) * 100)));
        }

        if (features.hasFailedPayments) {
            factors.add(new RiskFactor(
                    "Payment Issues",
                    -0.9,
                    "Recent failed payment attempts detected"));
        }

        if (features.tenureMonths < 3) {
            factors.add(new RiskFactor(
                    "New Customer",
                    -0.6,
                    "Customer is in high-risk onboarding period"));
        }

        return factors;
    }

    private List<RetentionStrategy> generateRetentionStrategies(
            ChurnFeatures features,
            List<RiskFactor> riskFactors) {

        List<RetentionStrategy> strategies = new ArrayList<>();

        // Personalized strategies based on risk factors
        for (RiskFactor factor : riskFactors) {
            switch (factor.factor) {
                case "Low Engagement":
                    strategies.add(new RetentionStrategy(
                            "Engagement Campaign",
                            0.25,
                            "Send personalized onboarding emails with video tutorials",
                            1));
                    strategies.add(new RetentionStrategy(
                            "Success Manager Outreach",
                            0.35,
                            "Assign dedicated success manager for onboarding call",
                            1));
                    break;

                case "Declining Usage":
                    strategies.add(new RetentionStrategy(
                            "Feature Discovery",
                            0.30,
                            "Highlight unused features via in-app notifications",
                            2));
                    strategies.add(new RetentionStrategy(
                            "Use Case Workshop",
                            0.40,
                            "Offer free workshop to discover new use cases",
                            1));
                    break;

                case "Payment Issues":
                    strategies.add(new RetentionStrategy(
                            "Payment Resolution",
                            0.50,
                            "Proactive outreach to resolve payment method issues",
                            1));
                    strategies.add(new RetentionStrategy(
                            "Flexible Payment Terms",
                            0.35,
                            "Offer installment plan or net terms",
                            2));
                    break;

                case "New Customer":
                    strategies.add(new RetentionStrategy(
                            "White Glove Onboarding",
                            0.45,
                            "Provide dedicated onboarding specialist",
                            1));
                    break;
            }
        }

        // General retention strategies
        if (features.planTier < PlanTier.PROFESSIONAL.ordinal()) {
            strategies.add(new RetentionStrategy(
                    "Upgrade Incentive",
                    0.20,
                    "Offer 20% discount on next tier upgrade",
                    3));
        }

        // Sort by priority
        strategies.sort(Comparator.comparingInt(s -> s.priority));

        return strategies.stream().limit(5).collect(Collectors.toList());
    }
}
