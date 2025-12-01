
/**
 * Semantic memory - stores long-term knowledge
 */
@ApplicationScoped
public class SemanticMemory {
    private final VectorStore vectorStore;
    
    public void store(MemoryEntry entry, float[] embedding) {
        vectorStore.upsert(entry.getId(), embedding, entry.toMap());
    }
    
    public List<MemoryEntry> search(float[] queryEmbedding, int topK) {
        return vectorStore.search(queryEmbedding, topK);
    }
}