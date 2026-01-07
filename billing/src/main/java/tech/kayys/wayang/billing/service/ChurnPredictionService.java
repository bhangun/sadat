package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.domain.ChurnPrediction;
import tech.kayys.wayang.billing.model.ModelAccuracy;
import tech.kayys.wayang.billing.model.RiskLevel;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.model.OrganizationStatus;

/**
 * Churn prediction service
 */
@ApplicationScoped
public class ChurnPredictionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChurnPredictionService.class);
    
    @Inject
    FeatureEngineeringService featureService;
    
    @Inject
    MLModelService modelService;
    
    @Inject
    NotificationService notificationService;
    
    /**
     * Predict churn for organization
     */
    public Uni<ChurnPrediction> predictChurn(UUID organizationId) {
        LOG.info("Predicting churn for organization: {}", organizationId);
        
        return Organization.<Organization>findById(organizationId)
            .flatMap(org -> {
                if (org == null) {
                    return Uni.createFrom().failure(
                        new NoSuchElementException("Organization not found"));
                }
                
                return featureService.extractFeatures(org)
                    .flatMap(features -> 
                        modelService.predictChurn(features)
                    )
                    .flatMap(prediction -> 
                        savePrediction(org, prediction)
                    )
                    .flatMap(prediction -> {
                        // Alert if high risk
                        if (prediction.riskLevel.ordinal() >= RiskLevel.HIGH.ordinal()) {
                            return notificationService.sendChurnAlert(prediction)
                                .replaceWith(prediction);
                        }
                        return Uni.createFrom().item(prediction);
                    });
            });
    }
    
    /**
     * Batch prediction for all active organizations
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void batchPredictChurn() {
        LOG.info("Running batch churn predictions");
        
        Organization.<Organization>find("status = ?1 and activeSubscription is not null", 
            OrganizationStatus.ACTIVE)
        .list()
        .subscribe().with(
            organizations -> {
                LOG.info("Predicting churn for {} organizations", organizations.size());
                organizations.forEach(org -> 
                    predictChurn(org.organizationId)
                        .subscribe().with(
                            prediction -> LOG.info("Predicted churn for {}: {}", 
                                org.tenantId, prediction.churnProbability),
                            error -> LOG.error("Error predicting churn for {}", 
                                org.tenantId, error)
                        )
                );
            },
            error -> LOG.error("Error in batch churn prediction", error)
        );
    }
    
    /**
     * Get churn predictions with filters
     */
    public Uni<List<ChurnPrediction>> getChurnPredictions(
            RiskLevel minRiskLevel,
            int limit) {
        
        return ChurnPrediction.<ChurnPrediction>find(
            "riskLevel >= ?1 and predictionDate >= ?2 order by churnProbability desc",
            minRiskLevel,
            Instant.now().minus(7, ChronoUnit.DAYS)
        ).page(0, limit)
        .list();
    }
    
    /**
     * Validate prediction accuracy
     */
    public Uni<ModelAccuracy> validatePredictions(YearMonth month) {
        return ChurnPrediction.<ChurnPrediction>find(
            "predictionDate >= ?1 and predictionDate < ?2 and isActualChurn is not null",
            month.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
            month.atEndOfMonth().atTime(23, 59).toInstant(java.time.ZoneOffset.UTC)
        ).list()
        .map(predictions -> {
            int truePositives = 0;
            int falsePositives = 0;
            int trueNegatives = 0;
            int falseNegatives = 0;
            
            for (ChurnPrediction pred : predictions) {
                boolean predicted = pred.churnProbability > 0.5;
                boolean actual = pred.isActualChurn;
                
                if (predicted && actual) truePositives++;
                else if (predicted && !actual) falsePositives++;
                else if (!predicted && actual) falseNegatives++;
                else trueNegatives++;
            }
            
            double accuracy = (double) (truePositives + trueNegatives) / predictions.size();
            double precision = truePositives > 0 ? 
                (double) truePositives / (truePositives + falsePositives) : 0;
            double recall = truePositives > 0 ?
                (double) truePositives / (truePositives + falseNegatives) : 0;
            double f1Score = precision + recall > 0 ?
                2 * (precision * recall) / (precision + recall) : 0;
            
            return new ModelAccuracy(accuracy, precision, recall, f1Score);
        });
    }
    
    private Uni<ChurnPrediction> savePrediction(
            Organization org,
            ChurnPrediction prediction) {
        
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            prediction.organization = org;
            prediction.predictionDate = Instant.now();
            return prediction.persist()
                .map(v -> prediction);
        });
    }
}
