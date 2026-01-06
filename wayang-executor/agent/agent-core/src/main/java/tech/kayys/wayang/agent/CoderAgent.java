package tech.kayys.wayang.agent;

import java.util.Set;

/**
 * Coder Agent - Code generation and analysis
 */
public record CoderAgent(
    Set<String> programmingLanguages,
    Set<CodeCapability> codeCapabilities,
    String executionEnvironment
) implements AgentType {
    
    @Override
    public String typeName() {
        return "CODER_AGENT";
    }
    
    @Override
    public Set<AgentCapability> requiredCapabilities() {
        return Set.of(
            AgentCapability.CODE_GENERATION,
            AgentCapability.CODE_ANALYSIS,
            AgentCapability.TOOL_USE
        );
    }
    
    @Override
    public AgentRole role() {
        return AgentRole.CODER;
    }
}
