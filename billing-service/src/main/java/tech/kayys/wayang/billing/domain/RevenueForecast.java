package tech.kayys.wayang.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import tech.kayys.wayang.billing.model.ForecastScenario;
import tech.kayys.wayang.billing.model.RevenueComponents;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

/**
 * ============================================================================
 * REVENUE FORECASTING ENGINE
 * ============================================================================
 * 
 * Advanced revenue forecasting with:
 * - Time series analysis
 * - Seasonal decomposition
 * - Growth trend modeling
 * - Cohort analysis
 * - Scenario planning
 */

// ==================== REVENUE FORECAST ====================

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "ai_revenue_forecasts")
public class RevenueForecast extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID forecastId;

    @Column(name = "forecast_month")
    public YearMonth forecastMonth;

    @Column(name = "forecast_date")
    public Instant forecastDate;

    @Column(name = "predicted_mrr", precision = 19, scale = 4)
    public BigDecimal predictedMRR;

    @Column(name = "predicted_arr", precision = 19, scale = 4)
    public BigDecimal predictedARR;

    @Column(name = "confidence_interval_lower", precision = 19, scale = 4)
    public BigDecimal confidenceLower;

    @Column(name = "confidence_interval_upper", precision = 19, scale = 4)
    public BigDecimal confidenceUpper;

    @Column(name = "confidence_level")
    public double confidenceLevel = 0.95;

    @Enumerated(EnumType.STRING)
    @Column(name = "forecast_scenario")
    public ForecastScenario scenario;

    @Column(name = "components", columnDefinition = "jsonb")
    public RevenueComponents components;

    @Column(name = "assumptions", columnDefinition = "jsonb")
    public Map<String, Object> assumptions;

    @Column(name = "model_metrics", columnDefinition = "jsonb")
    public Map<String, Double> modelMetrics;

    @Column(name = "actual_mrr", precision = 19, scale = 4)
    public BigDecimal actualMRR; // Filled in after month ends
}