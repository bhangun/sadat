package tech.kayys.agent.schema;

import java.util.List;

public record RiskProfileExtension(String level, List<String> allowedTools)
    implements AgentExtension {}