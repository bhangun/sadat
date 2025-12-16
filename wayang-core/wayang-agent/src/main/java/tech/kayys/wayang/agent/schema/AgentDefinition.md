package tech.kayys.agent.schema;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import tech.kayys.agent.core.AgentType;

public record AgentDefinition(
        String id,
        String name,
        Optional<String> description,
        AgentType type,
        List<ToolSpec> tools,
        Optional<MemoryConfig> memory,
        SecurityContext security,
        List<String> allowedActions,
        List<ExtensionPoint> extensions,
        URI schemaRef,
        Optional<String> checksum) {
    public AgentDefinition {
        // Defensive copies
        tools = List.copyOf(tools);
        extensions = List.copyOf(extensions);
        allowedActions = List.copyOf(allowedActions);
    }
}
