
/**
 * Embedding result with metadata
 */
@RegisterForReflection
public class EmbeddingResult {
    private final float[] embedding;
    private final String text;
    private final int dimension;
    private final String model;
    private final String checksum;
    private final Instant createdAt;
    
    private EmbeddingResult(Builder builder) {
        this.embedding = Objects.requireNonNull(builder.embedding);
        this.text = builder.text;
        this.dimension = builder.dimension;
        this.model = builder.model;
        this.checksum = builder.checksum;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }
    
    // Getters
    public float[] getEmbedding() { return embedding; }
    public String getText() { return text; }
    public int getDimension() { return dimension; }
    public String getModel() { return model; }
    public String getChecksum() { return checksum; }
    public Instant getCreatedAt() { return createdAt; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float[] embedding;
        private String text;
        private int dimension;
        private String model;
        private String checksum;
        private Instant createdAt;
        
        public Builder embedding(float[] embedding) {
            this.embedding = embedding;
            return this;
        }
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder dimension(int dimension) {
            this.dimension = dimension;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder checksum(String checksum) {
            this.checksum = checksum;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public EmbeddingResult build() {
            return new EmbeddingResult(this);
        }
    }
}