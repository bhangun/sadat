package tech.kayys.wayang.agent;

import java.util.Set;

/**
 * Analytics Agent - Data analysis and insights
 */
public record AnalyticsAgent(
    Set<AnalyticsCapability> analyticsCapabilities,
    Set<String> supportedDataFormats,
    boolean supportsVisualization
) implements AgentType {
    
    @Override
    public String typeName() {
        return "ANALYTICS_AGENT";
    }
    
    @Override
    public Set<AgentCapability> requiredCapabilities() {
        return Set.of(
            AgentCapability.DATA_ANALYSIS,
            AgentCapability.REASONING,
            AgentCapability.TOOL_USE
        );
    }
    
    @Override
    public AgentRole role() {
        return AgentRole.ANALYST;
    }
}
