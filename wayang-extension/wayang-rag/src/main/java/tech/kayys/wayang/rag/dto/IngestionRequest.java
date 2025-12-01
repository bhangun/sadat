
/**
 * Ingestion request
 */
@Immutable
public final class IngestionRequest {
    private final String sourceId;
    private final String tenantId;
    private final List<Document> documents;
    private final IngestionConfig config;
    private final Map<String, String> metadata;
    
    private IngestionRequest(Builder builder) {
        this.sourceId = requireNonNull(builder.sourceId);
        this.tenantId = requireNonNull(builder.tenantId);
        this.documents = List.copyOf(builder.documents);
        this.config = builder.config;
        this.metadata = Map.copyOf(builder.metadata);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String sourceId;
        private String tenantId;
        private List<Document> documents = new ArrayList<>();
        private IngestionConfig config = IngestionConfig.defaults();
        private Map<String, String> metadata = new HashMap<>();
        
        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder document(Document document) {
            this.documents.add(document);
            return this;
        }
        
        public Builder documents(List<Document> documents) {
            this.documents.addAll(documents);
            return this;
        }
        
        public Builder config(IngestionConfig config) {
            this.config = config;
            return this;
        }
        
        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public IngestionRequest build() {
            return new IngestionRequest(this);
        }
    }
}