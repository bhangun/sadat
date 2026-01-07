package tech.kayys.wayang.billing.model;

/**
 * Risk factors contributing to churn
 */
public class RiskFactor {
    public String factor;
    public double impact; // -1.0 to 1.0
    public String description;
    
    public RiskFactor(String factor, double impact, String description) {
        this.factor = factor;
        this.impact = impact;
        this.description = description;
    }
}