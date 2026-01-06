package tech.kayys.wayang.agent;

import java.util.Set;

/**
 * Common Agent - General-purpose task execution
 */
public record CommonAgent(
    String specialization,
    Set<String> supportedTasks
) implements AgentType {
    
    @Override
    public String typeName() {
        return "COMMON_AGENT";
    }
    
    @Override
    public Set<AgentCapability> requiredCapabilities() {
        return Set.of(
            AgentCapability.REASONING,
            AgentCapability.TOOL_USE
        );
    }
    
    @Override
    public AgentRole role() {
        return AgentRole.EXECUTOR;
    }
}