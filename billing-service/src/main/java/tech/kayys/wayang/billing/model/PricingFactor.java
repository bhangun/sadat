package tech.kayys.wayang.billing.model;

public class PricingFactor {
    public String name;
    public double impact; // -1.0 to 1.0
    public String rationale;
    
    public PricingFactor(String name, double impact, String rationale) {
        this.name = name;
        this.impact = impact;
        this.rationale = rationale;
    }
}