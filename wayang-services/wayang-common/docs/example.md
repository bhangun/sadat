


```java
ObjectMapper mapper = new ObjectMapper();

JsonNode pineconeParams = mapper.createObjectNode()
    .put("cloud", "aws")
    .put("region", "us-west-2");

VectorStoreConfig pineconeConfig = new VectorStoreConfig(
    storeId = "pinecone-prod",
    schemaUri = URI.create("https://schemas.example.com/vectorstore/pinecone-v1"),
    indexName = "agent-memories",
    namespace = "tenant-abc",
    embeddingModelRef = "model://openai/text-embedding-3-small",
    parameters = pineconeParams,
    secretRef = "vault://prod/pinecone/api-key"
);

MemoryConfig memory = new VectorStoreMemoryConfig(
    storeType = VectorStoreType.PINECONE,
    config = pineconeConfig,
    topK = 5,
    similarityThreshold = 0.75
);

AgentDefinition agent = AgentDefinition.builder()
    .id("agent-1")
    .name("Research Assistant")
    .memory(Optional.of(memory))
    // ... other fields
    .build();
```



📦 Optional: Add JSON Schema Validation Later

You can validate parameters against a store-specific JSON Schema using:

```java
// At runtime, for Pinecone:
JsonSchema pineconeSchema = ... // loaded from schemaUri
if (!pineconeSchema.validate(pineconeConfig.parameters()).isSuccess()) {
    throw new IllegalArgumentException("Invalid Pinecone parameters");
}
(Use libraries like networknt/json-schema-validator )
```



```java
ObjectMapper mapper = new ObjectMapper();

JsonNode calcParams = mapper.createObjectNode()
    .put("max_digits", 10)
    .put("allow_float", true);

AgentCapability cap = AgentCapability.builder()
    .capabilityId("calc-v1")
    .agentId("agent-123")
    .name("Calculator")
    .description("Evaluates math expressions")
    .addPermission("tool:calculator:execute")
    .parameters(calcParams)
    .addMethod("POST")
    .addDomain("math")
    .endpoint(URI.create("https://tools.example.com/calc"))
    .status(AgentStatus.ACTIVE)
    .build();
```


