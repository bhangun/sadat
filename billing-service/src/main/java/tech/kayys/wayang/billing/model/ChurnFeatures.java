package tech.kayys.wayang.billing.model;

import java.util.UUID;

/**
 * Churn features for ML model
 */
public class ChurnFeatures {
    public UUID organizationId;
    public double engagementScore;
    public double usageTrend;
    public double billingHealth;
    public double supportScore;
    public double productAdoption;
    public int tenureMonths;
    public int planTier;
    public boolean hasFailedPayments;
    public boolean isTrialUser;
    
    /**
     * Convert to feature vector for ML model
     */
    public double[] toVector() {
        return new double[] {
            engagementScore / 100.0,
            usageTrend,
            billingHealth / 100.0,
            supportScore / 100.0,
            productAdoption / 100.0,
            Math.min(tenureMonths / 24.0, 1.0), // Normalize to 0-1
            planTier / 5.0,
            hasFailedPayments ? 1.0 : 0.0,
            isTrialUser ? 1.0 : 0.0
        };
    }
}
