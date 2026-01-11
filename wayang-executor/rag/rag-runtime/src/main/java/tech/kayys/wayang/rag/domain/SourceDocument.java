package tech.kayys.silat.executor.rag.domain;

import java.util.Map;

public class SourceDocument {
    private final String id;
    private final String title;
    private final String content;
    private final String sourceUri;
    private final Map<String, String> metadata;
    private final float similarityScore;
    private final int pageNumber;
    private final String sectionTitle;

    public SourceDocument(String id, String title, String content, String sourceUri, 
                         Map<String, String> metadata, float similarityScore, int pageNumber, 
                         String sectionTitle) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.sourceUri = sourceUri;
        this.metadata = metadata;
        this.similarityScore = similarityScore;
        this.pageNumber = pageNumber;
        this.sectionTitle = sectionTitle;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSourceUri() { return sourceUri; }
    public Map<String, String> getMetadata() { return metadata; }
    public float getSimilarityScore() { return similarityScore; }
    public int getPageNumber() { return pageNumber; }
    public String getSectionTitle() { return sectionTitle; }
}