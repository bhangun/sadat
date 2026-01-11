package tech.kayys.wayang.billing.model;

/**
 * Quota status tracker
 */
public class QuotaStatus {
    public long used;
    public long limit;
    public double percentUsed;
    public boolean exceeded;
    
    public QuotaStatus(long used, long limit) {
        this.used = used;
        this.limit = limit;
        this.percentUsed = limit > 0 ? (used * 100.0 / limit) : 0;
        this.exceeded = used > limit;
    }
}
