
## 📦 Complete LangChain4j Integration

### 1. **Main Executor** (`silat_rag_langchain4j`)
- Full RAG pipeline using LangChain4j components
- Multi-provider support (OpenAI, Anthropic, Azure OpenAI)
- Advanced retrieval with query transformation
- Multiple vector stores (PgVector, Pinecone, Weaviate)
- Custom query transformers and routers
- Metrics collection and caching

### 2. **Document Ingestion Service** (`silat_rag_langchain4j_usage`)
- PDF document ingestion with Apache PDFBox
- Text document ingestion
- URL scraping support
- Batch ingestion from multiple sources
- Custom chunking strategies
- Metadata management

### 3. **Query Service**
- Simple RAG queries
- Advanced queries with full configuration
- Conversational RAG with history
- Multi-turn dialogue support

### 4. **Complete Examples**
- Example 1: Simple document ingestion and query
- Example 2: Advanced RAG with custom configuration
- Example 3: Conversational RAG with context
- Example 4: Batch document ingestion

## ✨ Key Features

### LangChain4j Components Used:
- ✅ **ChatLanguageModel** - OpenAI, Anthropic, Azure OpenAI
- ✅ **EmbeddingModel** - Multiple providers
- ✅ **EmbeddingStore** - PgVector, Pinecone, Weaviate
- ✅ **DocumentSplitter** - Recursive, sentence-based
- ✅ **DocumentParser** - PDF, text, custom formats
- ✅ **ContentRetriever** - Advanced retrieval with filters
- ✅ **RetrievalAugmentor** - Query transformation and routing
- ✅ **QueryTransformer** - Multi-query generation
- ✅ **QueryRouter** - Collection-based routing
- ✅ **EmbeddingStoreIngestor** - Batch ingestion

### Advanced Capabilities:
- **Multi-tenant isolation** with namespace separation
- **Hybrid search** combining vector and keyword
- **Query transformation** for better retrieval
- **Conversation history** for multi-turn dialogues
- **Metadata filtering** for precise retrieval
- **Citation tracking** with multiple styles
- **Comprehensive metrics** for observability
- **Redis caching** for performance
- **Streaming responses** (ready for implementation)

### Comparison with Custom Implementation:

| Feature | Custom RAG | LangChain4j RAG |
|---------|-----------|-----------------|
| **Setup Complexity** | High | Medium |
| **Code Maintenance** | Manual | Framework-handled |
| **Document Parsing** | Custom | Built-in parsers |
| **Provider Support** | Manual REST clients | Native integrations |
| **Vector Stores** | Custom implementations | Native support |
| **Query Enhancement** | Manual | Built-in transformers |
| **Extensibility** | High | Very High |
| **Community Support** | Internal | LangChain4j ecosystem |

## 🚀 Usage

```java
// 1. Ingest documents
DocumentIngestionService ingestion = ...;
ingestion.ingestPdfDocuments(
    "acme-corp",
    List.of(Path.of("/docs/manual.pdf")),
    Map.of("collection", "docs")
).await().indefinitely();

// 2. Query
RagQueryService queryService = ...;
RagResponse response = queryService.query(
    "acme-corp",
    "How do I reset my password?",
    "docs"
).await().indefinitely();

System.out.println(response.answer());
```

## 📚 Dependencies

Add to your `pom.xml`:
```xml
<!-- LangChain4j Core + Providers -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.35.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.35.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>0.35.0</version>
</dependency>
```

This implementation gives you the **best of both worlds**: the power and flexibility of LangChain4j combined with Silat's workflow orchestration capabilities! 🎉