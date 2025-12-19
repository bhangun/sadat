package tech.kayys.agent.schema;

// VectorStoreType.java
public enum VectorStoreType {
    PINECONE,
    WEAVIATE,
    QDRANT,
    MILVUS,
    CUSTOM; // for future or internal stores

    public static VectorStoreType fromString(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
}