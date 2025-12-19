package tech.kayys.wayang.schema.agent;

/**
 * Agent Decision.
 */
class AgentDecision {
    private String reasoning;
    private String action;
    private String input;
    private java.time.Instant timestamp;

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public java.time.Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(java.time.Instant timestamp) {
        this.timestamp = timestamp;
    }
    
}