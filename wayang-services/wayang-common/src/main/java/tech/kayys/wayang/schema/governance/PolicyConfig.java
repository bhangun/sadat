package tech.kayys.wayang.schema.governance;

import java.util.Arrays;
import java.util.List;

public class PolicyConfig {
    private AccessPolicy accessPolicy;
    private RateLimit rateLimit;
    private Integer dataRetentionDays;
    private String piiHandling = "redact";
    private NetworkEgressPolicy networkEgressPolicy;

    public AccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Integer getDataRetentionDays() {
        return dataRetentionDays;
    }

    public void setDataRetentionDays(Integer dataRetentionDays) {
        if (dataRetentionDays != null && dataRetentionDays < 0) {
            throw new IllegalArgumentException("Data retention days cannot be negative");
        }
        this.dataRetentionDays = dataRetentionDays;
    }

    public String getPiiHandling() {
        return piiHandling;
    }

    public void setPiiHandling(String piiHandling) {
        List<String> validHandling = Arrays.asList("redact", "hash", "tokenize", "none");
        if (!validHandling.contains(piiHandling)) {
            throw new IllegalArgumentException("Invalid PII handling: " + piiHandling);
        }
        this.piiHandling = piiHandling;
    }

    public NetworkEgressPolicy getNetworkEgressPolicy() {
        return networkEgressPolicy;
    }

    public void setNetworkEgressPolicy(NetworkEgressPolicy networkEgressPolicy) {
        this.networkEgressPolicy = networkEgressPolicy;
    }
}
