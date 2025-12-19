package tech.kayys.wayang.schema.governance;

import java.util.List;

public class ProvenanceConfig {
    private Boolean enabled = true;
    private List<String> collect;
    private Integer retentionDays = 30;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getCollect() {
        return collect;
    }

    public void setCollect(List<String> collect) {
        this.collect = collect;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        if (retentionDays != null && retentionDays < 0) {
            throw new IllegalArgumentException("Retention days cannot be negative");
        }
        this.retentionDays = retentionDays;
    }
}
