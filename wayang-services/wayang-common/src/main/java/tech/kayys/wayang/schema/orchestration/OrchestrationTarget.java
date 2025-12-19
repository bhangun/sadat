package tech.kayys.wayang.schema.orchestration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.schema.agent.A2AConfig;

public class OrchestrationTarget {
    private String refType;
    private String ref;
    private Map<String, Object> inputMapping;
    private Map<String, Object> outputMapping;
    private String condition;
    private boolean required = true;
    private Integer timeoutMs;
    private A2AConfig a2a;

    public OrchestrationTarget() {
    }

    public OrchestrationTarget(String refType, String ref) {
        this.refType = refType;
        this.ref = ref;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        List<String> validRefTypes = Arrays.asList("agent", "node", "connector",
                "workflow", "external_agent");
        if (!validRefTypes.contains(refType)) {
            throw new IllegalArgumentException("Invalid ref type: " + refType);
        }
        this.refType = refType;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        if (ref == null || ref.trim().isEmpty()) {
            throw new IllegalArgumentException("Target ref cannot be empty");
        }
        this.ref = ref;
    }

    public Map<String, Object> getInputMapping() {
        return inputMapping;
    }

    public void setInputMapping(Map<String, Object> inputMapping) {
        this.inputMapping = inputMapping;
    }

    public Map<String, Object> getOutputMapping() {
        return outputMapping;
    }

    public void setOutputMapping(Map<String, Object> outputMapping) {
        this.outputMapping = outputMapping;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        if (timeoutMs != null && timeoutMs < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        this.timeoutMs = timeoutMs;
    }

    public A2AConfig getA2a() {
        return a2a;
    }

    public void setA2a(A2AConfig a2a) {
        this.a2a = a2a;
    }
}
