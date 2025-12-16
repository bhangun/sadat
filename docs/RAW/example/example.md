
## 3. EXAMPLE GENERATED AGENTS

### EXAMPLE 1: Document Q&A Agent (RAG Only)

#### Input Workflow Schema:

```json
{
  "name": "Document Q&A Agent",
  "version": "1.0.0",
  "nodes": [
    {
      "nodeId": "start",
      "nodeType": "START"
    },
    {
      "nodeId": "rag1",
      "nodeType": "RAG",
      "config": {
        "index": "company-docs",
        "topK": 5,
        "embeddingModel": "all-MiniLM-L6-v2"
      }
    },
    {
      "nodeId": "end",
      "nodeType": "END"
    }
  ],
  "edges": [
    { "from": "start", "to": "rag1" },
    { "from": "rag1", "to": "end" }
  ]
}
```

#### Generated Structure:

```
document-qa-agent/
├── pom.xml                          # Only RAG dependencies
├── src/
│   └── main/
│       ├── java/
│       │   └── tech/kayys/generated/
│       │       ├── StandaloneAgent.java
│       │       ├── AgentConfig.java
│       │       └── nodes/
│       │           └── RAGNodeImplementation.java
│       └── resources/
│           ├── application.properties
│           └── logback.xml
├── Dockerfile                       # Optimized Docker image
├── docker-compose.yml
└── README.md
```

#### Generated pom.xml (Actual):

```xml
<dependencies>
    <!-- Base: 2MB -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-minimal</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- RAG: 3MB -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-rag</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- RAG Node: 0.5MB -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-node-rag</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Vector lib: 1MB -->
    <dependency>
        <groupId>com.github.jbellis</groupId>
        <artifactId>jvector</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- TOTAL: ~6.5MB -->
</dependencies>
```

#### Usage:

```bash
# Build
mvn clean package

# Run
java -jar target/document-qa-agent.jar \
  --rag-index company-docs \
  --input "What is our refund policy?"

# Docker
docker build -t document-qa-agent .
docker run document-qa-agent --input "..."

# Native Image (optional)
mvn package -Pnative
./target/document-qa-agent-native --input "..."
```

---

### EXAMPLE 2: Smart Customer Support Agent

#### Input Workflow Schema:

```json
{
  "name": "Customer Support Agent",
  "nodes": [
    { "nodeId": "start", "nodeType": "START" },
    { 
      "nodeId": "classify", 
      "nodeType": "AGENT",
      "config": {
        "model": "gpt-4o-mini",
        "systemPrompt": "Classify customer query into: refund, technical, general"
      }
    },
    {
      "nodeId": "decision1",
      "nodeType": "DECISION",
      "config": {
        "condition": "classify.category == 'refund'"
      }
    },
    {
      "nodeId": "rag_refund",
      "nodeType": "RAG",
      "config": { "index": "refund-policy" }
    },
    {
      "nodeId": "rag_technical",
      "nodeType": "RAG",
      "config": { "index": "tech-docs" }
    },
    {
      "nodeId": "generate_response",
      "nodeType": "AGENT",
      "config": {
        "model": "gpt-4o",
        "systemPrompt": "Generate helpful customer response"
      }
    },
    {
      "nodeId": "guardrail",
      "nodeType": "GUARDRAILS",
      "config": {
        "checks": ["pii", "toxicity", "policy"]
      }
    },
    { "nodeId": "end", "nodeType": "END" }
  ],
  "edges": [...]
}
```

#### Generated Dependencies:

```xml
<dependencies>
    <!-- Base: 2MB -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-minimal</artifactId>
    </dependency>
    
    <!-- LLM: 5MB (for AGENT nodes) -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-llm</artifactId>
    </dependency>
    
    <!-- RAG: 3MB (for RAG nodes) -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-rag</artifactId>
    </dependency>
    
    <!-- Guardrails: 2MB (for GUARDRAILS node) -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-guardrails</artifactId>
    </dependency>
    
    <!-- Node implementations -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-node-agent</artifactId>
    </dependency>
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-node-rag</artifactId>
    </dependency>
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-node-decision</artifactId>
    </dependency>
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-node-guardrails</artifactId>
    </dependency>
    
    <!-- TOTAL: ~17MB (vs 250MB full platform) -->
</dependencies>
```

---

### EXAMPLE 3: Data Processing Agent (Tools Only)

#### Input Workflow:

```json
{
  "name": "Data ETL Agent",
  "nodes": [
    { "nodeId": "start", "nodeType": "START" },
    {
      "nodeId": "fetch_data",
      "nodeType": "TOOL",
      "config": {
        "toolId": "rest-api-get",
        "endpoint": "https://api.example.com/data"
      }
    },
    {
      "nodeId": "transform",
      "nodeType": "TOOL",
      "config": {
        "toolId": "json-transformer"
      }
    },
    {
      "nodeId": "store",
      "nodeType": "TOOL",
      "config": {
        "toolId": "database-insert",
        "connection": "${DB_URL}"
      }
    },
    { "nodeId": "end", "nodeType": "END" }
  ]
}
```

#### Generated Dependencies:

```xml
<dependencies>
    <!-- Base: 2MB -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-minimal</artifactId>
    </dependency>
    
    <!-- Tools: 2MB -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-runtime-tools</artifactId>
    </dependency>
    
    <!-- Tool Node: 0.5MB -->
    <dependency>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-node-tool</artifactId>
    </dependency>
    
    <!-- HTTP Client: 1MB -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
    </dependency>
    
    <!-- JDBC Driver: 1MB -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- TOTAL: ~6.5MB -->
</dependencies>
```

---

## 4. SIZE COMPARISON TABLE

| Workflow Type | Nodes Used | Full Platform | Standalone | Reduction |
|---------------|------------|---------------|------------|-----------|
| RAG Only | 1 RAG | 250MB | 6.5MB | **97.4%** |
| Simple LLM | 1 Agent | 250MB | 7.5MB | **97.0%** |
| LLM + Tool | 1 Agent, 1 Tool | 250MB | 14MB | **94.4%** |
| RAG + LLM | 1 RAG,