package tech.kayys.agent.schema;

public record HybridMemoryConfig(
    ConversationBufferConfig buffer,
    VectorStoreMemoryConfig vector
) implements MemoryConfig {}