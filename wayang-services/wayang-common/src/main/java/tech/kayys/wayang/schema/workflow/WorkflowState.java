package tech.kayys.wayang.schema.workflow;

import java.util.Arrays;
import java.util.List;

public class WorkflowState {
    private String persistence;
    private Integer ttlDays;
    private Boolean resumeOnRestart = true;

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        List<String> validPersistence = Arrays.asList("none", "in_memory", "database", "event_sourcing");
        if (!validPersistence.contains(persistence)) {
            throw new IllegalArgumentException("Invalid persistence type: " + persistence);
        }
        this.persistence = persistence;
    }

    public Integer getTtlDays() {
        return ttlDays;
    }

    public void setTtlDays(Integer ttlDays) {
        if (ttlDays != null && ttlDays < 0) {
            throw new IllegalArgumentException("TTL days cannot be negative");
        }
        this.ttlDays = ttlDays;
    }

    public Boolean getResumeOnRestart() {
        return resumeOnRestart;
    }

    public void setResumeOnRestart(Boolean resumeOnRestart) {
        this.resumeOnRestart = resumeOnRestart;
    }
}
