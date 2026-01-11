package tech.kayys.wayang.billing.model;

import java.util.Map;

/**
 * ML Model response
 */
public class MLPredictionResponse {
    public double probability;
    public double confidence;
    public String modelVersion;
    public Map<String, Double> featureImportance;
}

