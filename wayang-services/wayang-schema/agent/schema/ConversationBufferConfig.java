package tech.kayys.agent.schema;

public record ConversationBufferConfig(
    int maxTokens,
    boolean includeSystemMessages
) implements MemoryConfig {}
