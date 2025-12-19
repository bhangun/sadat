package tech.kayys.wayang.schema.execution;

import java.util.List;

public class Task {
    private String formRef;
    private String instructions;
    private List<String> decisionOptions;
    private String defaultDecision;

    public String getFormRef() {
        return formRef;
    }

    public void setFormRef(String formRef) {
        this.formRef = formRef;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<String> getDecisionOptions() {
        return decisionOptions;
    }

    public void setDecisionOptions(List<String> decisionOptions) {
        this.decisionOptions = decisionOptions;
    }

    public String getDefaultDecision() {
        return defaultDecision;
    }

    public void setDefaultDecision(String defaultDecision) {
        this.defaultDecision = defaultDecision;
    }
}
