package tech.kayys.silat.executor.rag.domain;

public class RagWorkflowInput {
    private final String tenantId;
    private final String query;
    private final RetrievalConfig retrievalConfig;
    private final GenerationConfig generationConfig;

    public RagWorkflowInput(String tenantId, String query, RetrievalConfig retrievalConfig, 
                           GenerationConfig generationConfig) {
        this.tenantId = tenantId;
        this.query = query;
        this.retrievalConfig = retrievalConfig;
        this.generationConfig = generationConfig;
    }

    // Getters
    public String tenantId() { return tenantId; }
    public String query() { return query; }
    public RetrievalConfig retrievalConfig() { return retrievalConfig; }
    public GenerationConfig generationConfig() { return generationConfig; }
}