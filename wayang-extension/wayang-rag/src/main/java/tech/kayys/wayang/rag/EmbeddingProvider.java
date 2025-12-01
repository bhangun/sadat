
/**
 * Embedding provider interface for pluggable implementations
 */
public interface EmbeddingProvider {
    /**
     * Embed single text
     */
    float[] embed(String text);
    
    /**
     * Embed multiple texts in batch
     */
    List<float[]> embedBatch(List<String> texts);
    
    /**
     * Get model name
     */
    String getModelName();
    
    /**
     * Get embedding dimension
     */
    int getDimension();
}