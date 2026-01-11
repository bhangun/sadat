package tech.kayys.wayang.billing.model;

 /**
 * Recommended retention strategies
 */
public class RetentionStrategy {
    public String strategy;
    public double expectedImpact;
    public String actionPlan;
    public int priority;
    
    public RetentionStrategy(String strategy, double expectedImpact, 
                            String actionPlan, int priority) {
        this.strategy = strategy;
        this.expectedImpact = expectedImpact;
        this.actionPlan = actionPlan;
        this.priority = priority;
    }
}
