package tech.kayys.silat.executor.rag.langchain;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LangChain4jConfig {
    // Configuration properties for LangChain4j
    private String openAiApiKey;
    private String anthropicApiKey;
    private String azureApiKey;
    private String azureEndpoint;
    private String azureChatDeployment;
    private String azureEmbeddingDeployment;
    private boolean logRequests = false;
    private boolean logResponses = false;
    private String vectorstoreBackend = "postgres";
    private int embeddingDimension = 1536;
    
    // PostgreSQL vector store configuration
    private String postgresHost = "localhost";
    private int postgresPort = 5432;
    private String postgresDatabase = "silat";
    private String postgresUser = "postgres";
    private String postgresPassword;
    private String postgresTable = "embeddings";
    
    // Pinecone vector store configuration
    private String pineconeApiKey;
    private String pineconeEnvironment;
    private String pineconeProjectId;
    private String pineconeIndex = "silat";
    
    // Weaviate vector store configuration
    private String weaviateApiKey;
    private String weaviateScheme = "https";
    private String weaviateHost;
    private String weaviateClassName = "Document";

    // Getters and setters
    public String getOpenAiApiKey() { return openAiApiKey; }
    public void setOpenAiApiKey(String openAiApiKey) { this.openAiApiKey = openAiApiKey; }
    
    public String getAnthropicApiKey() { return anthropicApiKey; }
    public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }
    
    public String getAzureApiKey() { return azureApiKey; }
    public void setAzureApiKey(String azureApiKey) { this.azureApiKey = azureApiKey; }
    
    public String getAzureEndpoint() { return azureEndpoint; }
    public void setAzureEndpoint(String azureEndpoint) { this.azureEndpoint = azureEndpoint; }
    
    public String getAzureChatDeployment() { return azureChatDeployment; }
    public void setAzureChatDeployment(String azureChatDeployment) { this.azureChatDeployment = azureChatDeployment; }
    
    public String getAzureEmbeddingDeployment() { return azureEmbeddingDeployment; }
    public void setAzureEmbeddingDeployment(String azureEmbeddingDeployment) { this.azureEmbeddingDeployment = azureEmbeddingDeployment; }
    
    public boolean isLogRequests() { return logRequests; }
    public void setLogRequests(boolean logRequests) { this.logRequests = logRequests; }
    
    public boolean isLogResponses() { return logResponses; }
    public void setLogResponses(boolean logResponses) { this.logResponses = logResponses; }
    
    public String getVectorstoreBackend() { return vectorstoreBackend; }
    public void setVectorstoreBackend(String vectorstoreBackend) { this.vectorstoreBackend = vectorstoreBackend; }
    
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    
    public String getPostgresHost() { return postgresHost; }
    public void setPostgresHost(String postgresHost) { this.postgresHost = postgresHost; }
    
    public int getPostgresPort() { return postgresPort; }
    public void setPostgresPort(int postgresPort) { this.postgresPort = postgresPort; }
    
    public String getPostgresDatabase() { return postgresDatabase; }
    public void setPostgresDatabase(String postgresDatabase) { this.postgresDatabase = postgresDatabase; }
    
    public String getPostgresUser() { return postgresUser; }
    public void setPostgresUser(String postgresUser) { this.postgresUser = postgresUser; }
    
    public String getPostgresPassword() { return postgresPassword; }
    public void setPostgresPassword(String postgresPassword) { this.postgresPassword = postgresPassword; }
    
    public String getPostgresTable() { return postgresTable; }
    public void setPostgresTable(String postgresTable) { this.postgresTable = postgresTable; }
    
    public String getPineconeApiKey() { return pineconeApiKey; }
    public void setPineconeApiKey(String pineconeApiKey) { this.pineconeApiKey = pineconeApiKey; }
    
    public String getPineconeEnvironment() { return pineconeEnvironment; }
    public void setPineconeEnvironment(String pineconeEnvironment) { this.pineconeEnvironment = pineconeEnvironment; }
    
    public String getPineconeProjectId() { return pineconeProjectId; }
    public void setPineconeProjectId(String pineconeProjectId) { this.pineconeProjectId = pineconeProjectId; }
    
    public String getPineconeIndex() { return pineconeIndex; }
    public void setPineconeIndex(String pineconeIndex) { this.pineconeIndex = pineconeIndex; }
    
    public String getWeaviateApiKey() { return weaviateApiKey; }
    public void setWeaviateApiKey(String weaviateApiKey) { this.weaviateApiKey = weaviateApiKey; }
    
    public String getWeaviateScheme() { return weaviateScheme; }
    public void setWeaviateScheme(String weaviateScheme) { this.weaviateScheme = weaviateScheme; }
    
    public String getWeaviateHost() { return weaviateHost; }
    public void setWeaviateHost(String weaviateHost) { this.weaviateHost = weaviateHost; }
    
    public String getWeaviateClassName() { return weaviateClassName; }
    public void setWeaviateClassName(String weaviateClassName) { this.weaviateClassName = weaviateClassName; }
}