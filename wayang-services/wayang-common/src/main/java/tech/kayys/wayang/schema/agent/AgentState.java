package tech.kayys.wayang.schema.agent;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.schema.llm.ToolDefinition;

/**
 * Agent State.
 */
class AgentState {
    private String goal;
    private String role;
    private int iteration;
    private int maxIterations;
    private List<AgentDecision> history;
    private List<ToolDefinition> availableTools;
    private boolean completed;
    private Map<String, Object> lastResult;

    public void incrementIteration() {
        this.iteration++;
    }

    public void addHistory(AgentDecision decision) {
        this.history.add(decision);
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public List<AgentDecision> getHistory() {
        return history;
    }

    public void setHistory(List<AgentDecision> history) {
        this.history = history;
    }

    public List<ToolDefinition> getAvailableTools() {
        return availableTools;
    }

    public void setAvailableTools(List<ToolDefinition> availableTools) {
        this.availableTools = availableTools;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Map<String, Object> getLastResult() {
        return lastResult;
    }

    public void setLastResult(Map<String, Object> lastResult) {
        this.lastResult = lastResult;
    }

}
