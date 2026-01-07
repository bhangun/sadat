package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.domain.Anomaly;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.billing.dto.AnomalySeverity;
import tech.kayys.wayang.billing.dto.AnomalyType;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.model.OrganizationStatus;

/**
 * Anomaly detection service
 */
@ApplicationScoped
public class AnomalyDetectionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AnomalyDetectionService.class);
    
    private static final double Z_SCORE_THRESHOLD = 3.0; // 3 standard deviations
    
    /**
     * Detect anomalies for organization
     */
    public Uni<List<Anomaly>> detectAnomalies(String tenantId) {
        return Organization.<Organization>find("tenantId", tenantId)
            .firstResult()
            .flatMap(org -> {
                if (org == null) {
                    return Uni.createFrom().item(List.of());
                }
                
                return Uni.combine().all()
                    .unis(
                        detectUsageAnomalies(org),
                        detectCostAnomalies(org),
                        detectPatternAnomalies(org)
                    )
                    .asTuple()
                    .map(tuple -> {
                        List<Anomaly> all = new ArrayList<>();
                        all.addAll(tuple.getItem1());
                        all.addAll(tuple.getItem2());
                        all.addAll(tuple.getItem3());
                        return all;
                    });
            });
    }
    
    /**
     * Detect usage anomalies
     */
    private Uni<List<Anomaly>> detectUsageAnomalies(Organization org) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        
        return UsageRecord.<UsageRecord>find(
            "organization = ?1 and timestamp >= ?2",
            org, thirtyDaysAgo
        ).list()
        .map(records -> {
            List<Anomaly> anomalies = new ArrayList<>();
            
            // Group by day
            Map<String, Long> dailyUsage = records.stream()
                .collect(Collectors.groupingBy(
                    r -> r.timestamp.truncatedTo(ChronoUnit.DAYS).toString(),
                    Collectors.summingLong(r -> r.quantity)
                ));
            
            // Calculate statistics
            List<Long> values = new ArrayList<>(dailyUsage.values());
            if (values.isEmpty()) return anomalies;
            
            double mean = values.stream().mapToLong(Long::longValue)
                .average().orElse(0.0);
            double stdDev = calculateStdDev(values, mean);
            
            // Check for anomalies
            for (Map.Entry<String, Long> entry : dailyUsage.entrySet()) {
                double zScore = (entry.getValue() - mean) / stdDev;
                
                if (Math.abs(zScore) > Z_SCORE_THRESHOLD) {
                    Anomaly anomaly = new Anomaly();
                    anomaly.organization = org;
                    anomaly.detectedAt = Instant.now();
                    anomaly.anomalyType = zScore > 0 ? 
                        AnomalyType.USAGE_SPIKE : AnomalyType.USAGE_DROP;
                    anomaly.severity = Math.abs(zScore) > 5 ? 
                        AnomalySeverity.CRITICAL : AnomalySeverity.WARNING;
                    anomaly.description = String.format(
                        "Daily usage %s by %.1f standard deviations",
                        zScore > 0 ? "spike" : "drop",
                        Math.abs(zScore)
                    );
                    anomaly.metricName = "daily_usage";
                    anomaly.expectedValue = mean;
                    anomaly.actualValue = entry.getValue();
                    anomaly.deviationScore = Math.abs(zScore);
                    anomaly.context = Map.of("date", entry.getKey());
                    
                    anomalies.add(anomaly);
                }
            }
            
            return anomalies;
        });
    }
    
    /**
     * Detect cost anomalies
     */
    private Uni<List<Anomaly>> detectCostAnomalies(Organization org) {
        // Similar to usage anomalies but for costs
        return Uni.createFrom().item(List.of());
    }
    
    /**
     * Detect unusual patterns (ML-based)
     */
    private Uni<List<Anomaly>> detectPatternAnomalies(Organization org) {
        // TODO: Integrate with ML model for pattern recognition
        return Uni.createFrom().item(List.of());
    }
    
    private double calculateStdDev(List<Long> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    /**
     * Continuous anomaly monitoring
     */
    @Scheduled(every = "1h")
    public void monitorAnomalies() {
        LOG.info("Running anomaly detection scan");
        
        Organization.<Organization>find("status", OrganizationStatus.ACTIVE)
            .list()
            .subscribe().with(
                organizations -> organizations.forEach(org ->
                    detectAnomalies(org.tenantId)
                        .flatMap(anomalies -> saveAnomalies(anomalies))
                        .subscribe().with(
                            saved -> {
                                if (!saved.isEmpty()) {
                                    LOG.warn("Detected {} anomalies for {}", 
                                        saved.size(), org.tenantId);
                                }
                            },
                            error -> LOG.error("Error detecting anomalies", error)
                        )
                ),
                error -> LOG.error("Error in anomaly monitoring", error)
            );
    }
    
    private Uni<List<Anomaly>> saveAnomalies(List<Anomaly> anomalies) {
        if (anomalies.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() ->
            Uni.join().all(
                anomalies.stream()
                    .map(a -> a.persist())
                    .collect(Collectors.toList())
            ).andFailFast()
            .replaceWith(anomalies)
        );
    }
}
