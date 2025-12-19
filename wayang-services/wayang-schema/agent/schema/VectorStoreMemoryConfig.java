package tech.kayys.agent.schema;

public record VectorStoreMemoryConfig(
    VectorStoreType storeType,
    VectorStoreConfig config,
    int topK,               // number of results to retrieve
    double similarityThreshold
) implements MemoryConfig {}