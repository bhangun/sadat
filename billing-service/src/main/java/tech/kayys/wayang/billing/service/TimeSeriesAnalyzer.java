package tech.kayys.wayang.billing.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.model.MonthlyRevenue;
import tech.kayys.wayang.billing.model.TimeSeriesComponents;

/**
 * Time series analyzer
 */
@ApplicationScoped
public class TimeSeriesAnalyzer {
    
    /**
     * Decompose time series into trend, seasonal, and residual
     */
    public TimeSeriesComponents decompose(List<MonthlyRevenue> data) {
        // Calculate trend using moving average
        List<Double> trend = calculateMovingAverage(
            data.stream().map(MonthlyRevenue::value).collect(Collectors.toList()),
            3
        );
        
        // Calculate seasonal component
        Map<Integer, Double> seasonal = calculateSeasonalFactors(data, trend);
        
        // Calculate residuals
        List<Double> residuals = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (i < trend.size()) {
                double seasonalFactor = seasonal.get(
                    data.get(i).month().getMonthValue());
                residuals.add(data.get(i).value() - trend.get(i) * seasonalFactor);
            }
        }
        
        // Calculate R²
        double rSquared = calculateRSquared(
            data.stream().map(MonthlyRevenue::value).collect(Collectors.toList()),
            trend,
            seasonal
        );
        
        return new TimeSeriesComponents(trend, seasonal, residuals, rSquared);
    }
    
    /**
     * Extrapolate trend N periods ahead
     */
    public double extrapolateTrend(List<Double> trend, long periodsAhead) {
        if (trend.isEmpty()) return 0.0;
        
        // Simple linear extrapolation
        int n = trend.size();
        if (n < 2) return trend.get(0);
        
        // Calculate average growth rate
        double growthRate = 0.0;
        for (int i = 1; i < n; i++) {
            growthRate += (trend.get(i) - trend.get(i-1)) / trend.get(i-1);
        }
        growthRate /= (n - 1);
        
        // Extrapolate
        double lastValue = trend.get(n - 1);
        return lastValue * Math.pow(1 + growthRate, periodsAhead);
    }
    
    /**
     * Get seasonal factor for specific month
     */
    public double getSeasonalFactor(Map<Integer, Double> seasonal, YearMonth month) {
        return seasonal.getOrDefault(month.getMonthValue(), 1.0);
    }
    
    /**
     * Calculate moving average
     */
    private List<Double> calculateMovingAverage(List<Double> data, int window) {
        List<Double> ma = new ArrayList<>();
        
        for (int i = 0; i < data.size(); i++) {
            int start = Math.max(0, i - window / 2);
            int end = Math.min(data.size(), i + window / 2 + 1);
            
            double sum = 0.0;
            for (int j = start; j < end; j++) {
                sum += data.get(j);
            }
            ma.add(sum / (end - start));
        }
        
        return ma;
    }
    
    /**
     * Calculate seasonal factors by month
     */
    private Map<Integer, Double> calculateSeasonalFactors(
            List<MonthlyRevenue> data,
            List<Double> trend) {
        
        Map<Integer, List<Double>> ratiosByMonth = new HashMap<>();
        
        for (int i = 0; i < Math.min(data.size(), trend.size()); i++) {
            int month = data.get(i).month().getMonthValue();
            double ratio = data.get(i).value() / trend.get(i);
            
            ratiosByMonth.computeIfAbsent(month, k -> new ArrayList<>())
                .add(ratio);
        }
        
        // Average ratios by month
        Map<Integer, Double> seasonal = new HashMap<>();
        for (Map.Entry<Integer, List<Double>> entry : ratiosByMonth.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
            seasonal.put(entry.getKey(), avg);
        }
        
        return seasonal;
    }
    
    /**
     * Calculate R-squared (goodness of fit)
     */
    private double calculateRSquared(
            List<Double> actual,
            List<Double> trend,
            Map<Integer, Double> seasonal) {
        
        double meanActual = actual.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double ssTotal = 0.0;
        double ssResidual = 0.0;
        
        for (int i = 0; i < Math.min(actual.size(), trend.size()); i++) {
            double predicted = trend.get(i) * seasonal.getOrDefault(
                (i % 12) + 1, 1.0);
            
            ssTotal += Math.pow(actual.get(i) - meanActual, 2);
            ssResidual += Math.pow(actual.get(i) - predicted, 2);
        }
        
        return 1.0 - (ssResidual / ssTotal);
    }
    
    /**
     * Calculate Mean Absolute Percentage Error
     */
    public double calculateMAPE(List<MonthlyRevenue> history) {
        if (history.size() < 2) return 0.0;
        
        double sumPercentError = 0.0;
        for (int i = 1; i < history.size(); i++) {
            double actual = history.get(i).value();
            double predicted = history.get(i-1).value(); // Naive forecast
            
            if (actual != 0) {
                sumPercentError += Math.abs((actual - predicted) / actual);
            }
        }
        
        return (sumPercentError / (history.size() - 1)) * 100;
    }
    
    /**
     * Calculate standard error
     */
    public double calculateStandardError(List<MonthlyRevenue> history) {
        if (history.size() < 2) return 0.0;
        
        List<Double> errors = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            errors.add(history.get(i).value() - history.get(i-1).value());
        }
        
        double mean = errors.stream().mapToDouble(Double::doubleValue)
            .average().orElse(0.0);
        double variance = errors.stream()
            .mapToDouble(e -> Math.pow(e - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
}