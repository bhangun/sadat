package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Request DTO for code generation
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeGenRequest {

    @NotBlank(message = "Target platform is required")
    private String target;

    private Map<String, Object> options;

    private boolean includeTests = true;

    private String jobId;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public boolean isIncludeTests() {
        return includeTests;
    }

    public void setIncludeTests(boolean includeTests) {
        this.includeTests = includeTests;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
