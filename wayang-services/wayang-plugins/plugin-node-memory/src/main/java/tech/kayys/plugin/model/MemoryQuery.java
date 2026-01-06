
public class MemoryQuery {
    private String query;
    private int topK;
    private List<MemoryType> types;
    private String tenantId;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private MemoryQuery query = new MemoryQuery();
        
        public Builder query(String q) {
            query.query = q;
            return this;
        }
        
        public Builder topK(int topK) {
            query.topK = topK;
            return this;
        }
        
        public Builder types(List<MemoryType> types) {
            query.types = types;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            query.tenantId = tenantId;
            return this;
        }
        
        public MemoryQuery build() {
            return query;
        }
    }
    
    // Getters
    public String getQuery() { return query; }
    public int getTopK() { return topK; }
    public List<MemoryType> getTypes() { return types; }
    public String getTenantId() { return tenantId; }
}