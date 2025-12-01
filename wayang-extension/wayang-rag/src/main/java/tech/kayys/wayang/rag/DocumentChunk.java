
/**
 * Document chunk
 */
@Immutable
public final class DocumentChunk {
    private final int index;
    private final String text;
    private final int startPosition;
    private final int endPosition;
    private final int tokenCount;
    private final Map<String, Object> metadata;
    
    private DocumentChunk(Builder builder) {
        this.index = builder.index;
        this.text = requireNonNull(builder.text);
        this.startPosition = builder.startPosition;
        this.endPosition = builder.endPosition;
        this.tokenCount = builder.tokenCount;
        this.metadata = Map.copyOf(builder.metadata);
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(metadata);
        map.put("index", index);
        map.put("text", text);
        map.put("startPosition", startPosition);
        map.put("endPosition", endPosition);
        map.put("tokenCount", tokenCount);
        return map;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int index;
        private String text;
        private int startPosition;
        private int endPosition;
        private int tokenCount;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder index(int index) {
            this.index = index;
            return this;
        }
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder startPosition(int startPosition) {
            this.startPosition = startPosition;
            return this;
        }
        
        public Builder endPosition(int endPosition) {
            this.endPosition = endPosition;
            return this;
        }
        
        public Builder tokenCount(int tokenCount) {
            this.tokenCount = tokenCount;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public DocumentChunk build() {
            return new DocumentChunk(this);
        }
    }
}