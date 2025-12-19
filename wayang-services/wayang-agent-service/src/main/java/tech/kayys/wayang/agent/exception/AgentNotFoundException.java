package tech.kayys.wayang.agent.exception;

public class AgentNotFoundException extends RuntimeException {
    public AgentNotFoundException(String agentId) {
        super("Agent not found: " + agentId);
    }
}