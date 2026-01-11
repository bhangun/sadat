package tech.kayys.wayang.billing.model;

import java.math.BigDecimal;

/**
 * Revenue components breakdown
 */
public class RevenueComponents {
    public BigDecimal newBusiness;
    public BigDecimal expansion;
    public BigDecimal renewal;
    public BigDecimal churn;
    public BigDecimal total;
    
    public RevenueComponents() {}
    
    public RevenueComponents(BigDecimal newBusiness, BigDecimal expansion,
                            BigDecimal renewal, BigDecimal churn) {
        this.newBusiness = newBusiness;
        this.expansion = expansion;
        this.renewal = renewal;
        this.churn = churn;
        this.total = newBusiness.add(expansion).add(renewal).subtract(churn);
    }
}


