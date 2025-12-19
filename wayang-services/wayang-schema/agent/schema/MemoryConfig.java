package tech.kayys.agent.schema;

// MemoryConfig.java
public sealed interface MemoryConfig
    permits ConversationBufferConfig, VectorStoreMemoryConfig, HybridMemoryConfig {}
