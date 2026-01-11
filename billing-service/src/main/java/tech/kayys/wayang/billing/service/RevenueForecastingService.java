package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.billing.domain.RevenueForecast;
import tech.kayys.wayang.billing.model.ForecastScenario;
import tech.kayys.wayang.billing.model.MonthlyRevenue;
import tech.kayys.wayang.billing.model.RevenueComponents;
import tech.kayys.wayang.billing.model.TimeSeriesComponents;

/**
 * Revenue forecasting service
 */
@ApplicationScoped
public class RevenueForecastingService {

        private static final Logger LOG = LoggerFactory.getLogger(RevenueForecastingService.class);

        @Inject
        TimeSeriesAnalyzer timeSeriesAnalyzer;

        @Inject
        CohortAnalyzer cohortAnalyzer;

        /**
         * Generate forecast for next N months
         */
        public Uni<List<RevenueForecast>> forecastRevenue(int monthsAhead) {
                LOG.info("Generating revenue forecast for {} months", monthsAhead);

                return getHistoricalRevenue(24) // Get 24 months of history
                                .flatMap(history -> {
                                        List<Uni<RevenueForecast>> forecasts = new ArrayList<>();

                                        for (int i = 1; i <= monthsAhead; i++) {
                                                YearMonth targetMonth = YearMonth.now().plusMonths(i);

                                                // Generate all three scenarios
                                                forecasts.add(forecastMonth(targetMonth, history,
                                                                ForecastScenario.CONSERVATIVE));
                                                forecasts.add(forecastMonth(targetMonth, history,
                                                                ForecastScenario.REALISTIC));
                                                forecasts.add(forecastMonth(targetMonth, history,
                                                                ForecastScenario.OPTIMISTIC));
                                        }

                                        return Uni.join().all(forecasts).andFailFast();
                                })
                                .flatMap(forecasts -> io.quarkus.hibernate.reactive.panache.Panache
                                                .withTransaction(() -> Uni
                                                                .join().all(
                                                                                forecasts.stream()
                                                                                                .map(f -> f.persist())
                                                                                                .collect(Collectors
                                                                                                                .toList()))
                                                                .andFailFast()
                                                                .replaceWith(forecasts)));
        }

        /**
         * Forecast for specific month
         */
        private Uni<RevenueForecast> forecastMonth(
                        YearMonth targetMonth,
                        List<MonthlyRevenue> history,
                        ForecastScenario scenario) {

                return Uni.createFrom().item(() -> {
                        // Time series decomposition
                        TimeSeriesComponents components = timeSeriesAnalyzer.decompose(history);

                        // Calculate trend
                        double trendValue = timeSeriesAnalyzer.extrapolateTrend(
                                        components.trend(),
                                        ChronoUnit.MONTHS.between(YearMonth.now(), targetMonth));

                        // Get seasonal factor
                        double seasonalFactor = timeSeriesAnalyzer.getSeasonalFactor(
                                        components.seasonal(),
                                        targetMonth);

                        // Calculate base forecast
                        BigDecimal baseForecast = BigDecimal.valueOf(trendValue * seasonalFactor);

                        // Apply scenario adjustments
                        BigDecimal scenarioAdjustment = switch (scenario) {
                                case CONSERVATIVE -> baseForecast.multiply(BigDecimal.valueOf(0.85));
                                case REALISTIC -> baseForecast;
                                case OPTIMISTIC -> baseForecast.multiply(BigDecimal.valueOf(1.15));
                        };

                        // Calculate confidence interval
                        double standardError = timeSeriesAnalyzer.calculateStandardError(history);
                        double margin = 1.96 * standardError; // 95% confidence

                        // Break down into components
                        RevenueComponents revenueComponents = calculateComponents(
                                        scenarioAdjustment,
                                        scenario);

                        // Build forecast
                        RevenueForecast forecast = new RevenueForecast();
                        forecast.forecastMonth = targetMonth;
                        forecast.forecastDate = Instant.now();
                        forecast.scenario = scenario;
                        forecast.predictedMRR = scenarioAdjustment;
                        forecast.predictedARR = scenarioAdjustment.multiply(BigDecimal.valueOf(12));
                        forecast.confidenceLower = scenarioAdjustment.subtract(
                                        BigDecimal.valueOf(margin));
                        forecast.confidenceUpper = scenarioAdjustment.add(
                                        BigDecimal.valueOf(margin));
                        forecast.components = revenueComponents;
                        forecast.assumptions = buildAssumptions(scenario);
                        forecast.modelMetrics = Map.of(
                                        "mape", timeSeriesAnalyzer.calculateMAPE(history),
                                        "r_squared", components.rSquared());

                        return forecast;
                });
        }

        /**
         * Calculate revenue components
         */
        private RevenueComponents calculateComponents(
                        BigDecimal totalRevenue,
                        ForecastScenario scenario) {

                // Historical ratios
                double newBusinessRatio = 0.20;
                double expansionRatio = 0.15;
                double renewalRatio = 0.70;
                double churnRatio = 0.05;

                // Adjust based on scenario
                if (scenario == ForecastScenario.OPTIMISTIC) {
                        newBusinessRatio *= 1.2;
                        expansionRatio *= 1.15;
                        churnRatio *= 0.8;
                } else if (scenario == ForecastScenario.CONSERVATIVE) {
                        newBusinessRatio *= 0.8;
                        expansionRatio *= 0.85;
                        churnRatio *= 1.2;
                }

                BigDecimal newBusiness = totalRevenue.multiply(
                                BigDecimal.valueOf(newBusinessRatio));
                BigDecimal expansion = totalRevenue.multiply(
                                BigDecimal.valueOf(expansionRatio));
                BigDecimal renewal = totalRevenue.multiply(
                                BigDecimal.valueOf(renewalRatio));
                BigDecimal churn = totalRevenue.multiply(
                                BigDecimal.valueOf(churnRatio));

                return new RevenueComponents(newBusiness, expansion, renewal, churn);
        }

        /**
         * Build forecast assumptions
         */
        private Map<String, Object> buildAssumptions(ForecastScenario scenario) {
                Map<String, Object> assumptions = new HashMap<>();

                assumptions.put("churnRate", switch (scenario) {
                        case CONSERVATIVE -> 0.06;
                        case REALISTIC -> 0.05;
                        case OPTIMISTIC -> 0.04;
                });

                assumptions.put("growthRate", switch (scenario) {
                        case CONSERVATIVE -> 0.05;
                        case REALISTIC -> 0.08;
                        case OPTIMISTIC -> 0.12;
                });

                assumptions.put("averageContractValue", 1500.0);
                assumptions.put("conversionRate", 0.20);
                assumptions.put("expansionRate", 0.15);

                return assumptions;
        }

        /**
         * Get historical revenue data
         */
        private Uni<List<MonthlyRevenue>> getHistoricalRevenue(int months) {
                YearMonth startMonth = YearMonth.now().minusMonths(months);

                return Invoice.<Invoice>find(
                                "status = ?1 and invoiceDate >= ?2",
                                InvoiceStatus.PAID,
                                startMonth.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()).list()
                                .map(invoices -> {
                                        Map<YearMonth, BigDecimal> revenueByMonth = new TreeMap<>();

                                        for (Invoice invoice : invoices) {
                                                YearMonth month = YearMonth.from(
                                                                invoice.invoiceDate.atZone(java.time.ZoneOffset.UTC));
                                                revenueByMonth.merge(month, invoice.totalAmount, BigDecimal::add);
                                        }

                                        return revenueByMonth.entrySet().stream()
                                                        .map(e -> new MonthlyRevenue(e.getKey(), e.getValue()))
                                                        .collect(Collectors.toList());
                                });
        }

        /**
         * Validate forecast accuracy (scheduled job)
         */
        @Scheduled(cron = "0 0 3 1 * ?") // First day of month at 3 AM
        public void validateForecasts() {
                YearMonth lastMonth = YearMonth.now().minusMonths(1);

                LOG.info("Validating forecasts for {}", lastMonth);

                // Get actual revenue
                getActualRevenue(lastMonth)
                                .flatMap(actualMRR ->
                                // Update forecasts with actual
                                RevenueForecast.<RevenueForecast>find(
                                                "forecastMonth = ?1 and scenario = ?2",
                                                lastMonth,
                                                ForecastScenario.REALISTIC).firstResult()
                                                .flatMap(forecast -> {
                                                        if (forecast != null) {
                                                                forecast.actualMRR = actualMRR;
                                                                return forecast.persist()
                                                                                .replaceWith(calculateAccuracy(
                                                                                                forecast));
                                                        }
                                                        return Uni.createFrom().nullItem();
                                                }))
                                .subscribe().with(
                                                accuracy -> LOG.info("Forecast accuracy: {}%", accuracy),
                                                error -> LOG.error("Error validating forecasts", error));
        }

        private Uni<BigDecimal> getActualRevenue(YearMonth month) {
                return Invoice.<Invoice>find(
                                "status = ?1 and invoiceDate >= ?2 and invoiceDate < ?3",
                                InvoiceStatus.PAID,
                                month.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                                month.atEndOfMonth().atTime(23, 59).toInstant(java.time.ZoneOffset.UTC)).list()
                                .map(invoices -> invoices.stream()
                                                .map(inv -> inv.totalAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        private double calculateAccuracy(RevenueForecast forecast) {
                if (forecast.actualMRR == null)
                        return 0.0;

                BigDecimal error = forecast.predictedMRR.subtract(forecast.actualMRR).abs();
                BigDecimal percentError = error.divide(
                                forecast.actualMRR, 4, RoundingMode.HALF_UP);

                return 100.0 - percentError.doubleValue() * 100;
        }
}