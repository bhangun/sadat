
public class HITLDecision {
    private String action;
    private String reviewer;
    private String notes;
    private Map<String, Object> correctedData;
    
    // Getters and setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Map<String, Object> getCorrectedData() { return correctedData; }
    public void setCorrectedData(Map<String, Object> correctedData) { 
        this.correctedData = correctedData; 
    }
}