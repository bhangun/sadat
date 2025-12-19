package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ExecutionMetrics - Execution metrics
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionMetrics {

    private long durationMs;
    private int nodesExecuted;
    private int nodesFailed;
    private long tokensUsed;
    private double costUsd;

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getNodesExecuted() {
        return nodesExecuted;
    }

    public void setNodesExecuted(int nodesExecuted) {
        this.nodesExecuted = nodesExecuted;
    }

    public long getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(long tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public double getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(double costUsd) {
        this.costUsd = costUsd;
    }
}
