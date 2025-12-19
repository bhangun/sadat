package tech.kayys.agent.schema;

import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

// VectorStoreConfig.java
public record VectorStoreConfig(
    String storeId,                     // logical ID (e.g., "prod-user-embeddings")
    URI schemaUri,                      // e.g., "https://schemas.yourco.com/vectorstore/pinecone-v1"
    String indexName,                   // required for most stores
    String namespace,                   // optional (e.g., tenant ID)
    String embeddingModelRef,           // e.g., "model://openai/text-embedding-3-small"
    JsonNode parameters,                // store-specific safe params (e.g., {"cloud": "aws", "region": "us-east-1"})
    String secretRef                    // e.g., "vault://prod/vectorstore/pinecone-creds"
) {
    public VectorStoreConfig {
        Objects.requireNonNull(storeId);
        Objects.requireNonNull(indexName);
        Objects.requireNonNull(secretRef); // credentials must be externalized
        parameters = (parameters == null) ? JsonNodeFactory.instance.objectNode() : parameters;
    }
}