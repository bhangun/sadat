package tech.kayys.wayang.schema.orchestration;

import java.util.Arrays;
import java.util.List;

public class OrchestrationSpec {
    private String strategy;
    private String executionMode = "camel_route";
    private HumanEscalation humanEscalation;
    private List<OrchestrationTarget> targets;
    private String decisionPolicy;
    private Integer maxConcurrency = 4;
    private Integer timeoutMs = 60000;
    private String failureStrategy = "retry";
    private Boolean emitPlan = true;

    public OrchestrationSpec() {
    }

    public OrchestrationSpec(String strategy, List<OrchestrationTarget> targets) {
        this.strategy = strategy;
        this.targets = targets;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        List<String> validStrategies = Arrays.asList("sequential", "parallel", "conditional",
                "dynamic", "planner_executor", "map_reduce");
        if (!validStrategies.contains(strategy)) {
            throw new IllegalArgumentException("Invalid orchestration strategy: " + strategy);
        }
        this.strategy = strategy;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        List<String> validModes = Arrays.asList("in_process", "camel_route", "external_workflow");
        if (!validModes.contains(executionMode)) {
            throw new IllegalArgumentException("Invalid execution mode: " + executionMode);
        }
        this.executionMode = executionMode;
    }

    public HumanEscalation getHumanEscalation() {
        return humanEscalation;
    }

    public void setHumanEscalation(HumanEscalation humanEscalation) {
        this.humanEscalation = humanEscalation;
    }

    public List<OrchestrationTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<OrchestrationTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("Orchestration must have at least one target");
        }
        this.targets = targets;
    }

    public String getDecisionPolicy() {
        return decisionPolicy;
    }

    public void setDecisionPolicy(String decisionPolicy) {
        this.decisionPolicy = decisionPolicy;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(Integer maxConcurrency) {
        if (maxConcurrency != null && maxConcurrency < 1) {
            throw new IllegalArgumentException("Max concurrency must be at least 1");
        }
        this.maxConcurrency = maxConcurrency;
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

    public String getFailureStrategy() {
        return failureStrategy;
    }

    public void setFailureStrategy(String failureStrategy) {
        List<String> validStrategies = Arrays.asList("fail_fast", "retry", "skip",
                "fallback", "escalate");
        if (!validStrategies.contains(failureStrategy)) {
            throw new IllegalArgumentException("Invalid failure strategy: " + failureStrategy);
        }
        this.failureStrategy = failureStrategy;
    }

    public Boolean getEmitPlan() {
        return emitPlan;
    }

    public void setEmitPlan(Boolean emitPlan) {
        this.emitPlan = emitPlan;
    }
}
