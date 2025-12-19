package tech.kayys.wayang.schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * EscalationConfigDTO - Escalation configuration
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EscalationConfigDTO {

    private String onSeverityAtLeast = "ERROR"; // WARN, ERROR, CRITICAL
    private List<String> notify = new ArrayList<>();

    // Getters and setters...
    public String getOnSeverityAtLeast() {
        return onSeverityAtLeast;
    }

    public void setOnSeverityAtLeast(String onSeverityAtLeast) {
        this.onSeverityAtLeast = onSeverityAtLeast;
    }

    public List<String> getNotify() {
        return notify;
    }

    public void setNotify(List<String> notify) {
        this.notify = notify;
    }
}
