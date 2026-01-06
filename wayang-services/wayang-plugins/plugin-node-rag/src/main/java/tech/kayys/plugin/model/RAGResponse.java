
public class RAGResponse {
    private List<Document> documents;
    private List<Double> scores;
    private String context;
    private List<Citation> citations;
    
    // Getters and setters
    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }
    
    public List<Double> getScores() { return scores; }
    public void setScores(List<Double> scores) { this.scores = scores; }
    
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    
    public List<Citation> getCitations() { return citations; }
    public void setCitations(List<Citation> citations) { this.citations = citations; }
    
    public String assembleContext() {
        return context != null ? context : "";
    }
}
