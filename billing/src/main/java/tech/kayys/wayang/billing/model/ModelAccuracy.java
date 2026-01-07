package tech.kayys.wayang.billing.model;

/**
 * Model accuracy metrics
 */
public record ModelAccuracy(
    double accuracy,
    double precision,
    double recall,
    double f1Score
) {}