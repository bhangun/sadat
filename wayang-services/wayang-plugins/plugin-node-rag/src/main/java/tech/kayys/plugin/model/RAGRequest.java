
public class RAGRequest {
    private String query;
    private int topK;
    private String index;
    private boolean hybrid;
    private boolean rerank;
    private Map<String, Object> filters;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private RAGRequest request = new RAGRequest();
        
        public Builder query(String query) {
            request.query = query;
            return this;
        }
        
        public Builder topK(int topK) {
            request.topK = topK;
            return this;
        }
        
        public Builder index(String index) {
            request.index = index;
            return this;
        }
        
        public Builder hybrid(boolean hybrid) {
            request.hybrid = hybrid;
            return this;
        }
        
        public Builder rerank(boolean rerank) {
            request.rerank = rerank;
            return this;
        }
        
        public Builder filters(Map<String, Object> filters) {
            request.filters = filters;
            return this;
        }
        
        public RAGRequest build() {
            return request;
        }
    }
    
    // Getters
    public String getQuery() { return query; }
    public int getTopK() { return topK; }
    public String getIndex() { return index; }
    public boolean isHybrid() { return hybrid; }
    public boolean isRerank() { return rerank; }
    public Map<String, Object> getFilters() { return filters; }
}
