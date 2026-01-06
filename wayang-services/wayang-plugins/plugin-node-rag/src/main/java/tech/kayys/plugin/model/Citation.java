public class Citation {
    private String documentId;
    private String source;
    private Object title;
    private double score;
    
    public Citation(String documentId, String source, Object title, double score) {
        this.documentId = documentId;
        this.source = source;
        this.title = title;
        this.score = score;
    }
    
    // Getters
    public String getDocumentId() { return documentId; }
    public String getSource() { return source; }
    public Object getTitle() { return title; }
    public double getScore() { return score; }
}
