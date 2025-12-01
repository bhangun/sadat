package tech.kayys.agent.schema;

public record ToolSpec(
    String name,
    String secretRef // e.g., "vault://prod/agent-123/openai-key"
) {}