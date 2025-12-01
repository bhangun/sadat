
## 10. WAYANG-RAG MODULE

```
wayang-rag/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/rag/
    │   │       ├── api/
    │   │       │   └── RAGResource.java
    │   │       │
    │   │       ├── service/
    │   │       │   ├── RAGService.java
    │   │       │   ├── HybridRAGService.java
    │   │       │   └── RAGConfig.java
    │   │       │
    │   │       ├── ingestion/
    │   │       │   ├── IngestionPipeline.java
    │   │       │   ├── DocumentParser.java
    │   │       │   ├── Chunker.java
    │   │       │   └── ChunkingStrategy.java
    │   │       │
    │   │       ├── embedding/
    │   │       │   ├── EmbeddingService.java
    │   │       │   ├── EmbeddingModel.java
    │   │       │   └── VectorNormalizer.java
    │   │       │
    │   │       ├── store/
    │   │       │   ├── VectorStore.java
    │   │       │   ├── PgVectorStore.java
    │   │       │   ├── MilvusStore.java
    │   │       │   └── MetadataStore.java
    │   │       │
    │   │       ├── search/
    │   │       │   ├── SearchEngine.java
    │   │       │   ├── HybridSearchEngine.java
    │   │       │   ├── ReRanker.java
    │   │       │   └── FusionEngine.java
    │   │       │
    │   │       └── model/
    │   │           ├── Document.java
    │   │           ├── Chunk.java
    │   │           ├── SearchQuery.java
    │   │           └── SearchResult.java
    │   │
    │   └── resources/
    │       └── application.properties
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/rag/
                ├── ingestion/
                │   └── ChunkerTest.java
                └── search/
                    └── HybridSearchEngineTest.java
```
