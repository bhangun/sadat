
public class Document {
    private String id;
    private String content;
    private String source;
    private double score;
    private Map<String, Object> metadata;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
