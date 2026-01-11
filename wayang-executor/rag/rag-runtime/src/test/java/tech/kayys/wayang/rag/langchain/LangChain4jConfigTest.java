package tech.kayys.silat.executor.rag.langchain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LangChain4jConfigTest {

    private LangChain4jConfig config;

    @BeforeEach
    void setUp() {
        config = new LangChain4jConfig();
    }

    @Test
    void testLangChain4jConfig_GettersAndSetters() {
        // Test OpenAI configuration
        config.setOpenAiApiKey("test-openai-key");
        assertEquals("test-openai-key", config.getOpenAiApiKey());

        // Test Anthropic configuration
        config.setAnthropicApiKey("test-anthropic-key");
        assertEquals("test-anthropic-key", config.getAnthropicApiKey());

        // Test Azure configuration
        config.setAzureApiKey("test-azure-key");
        config.setAzureEndpoint("https://test.openai.azure.com/");
        config.setAzureChatDeployment("gpt-4");
        config.setAzureEmbeddingDeployment("text-embedding-ada-002");
        
        assertEquals("test-azure-key", config.getAzureApiKey());
        assertEquals("https://test.openai.azure.com/", config.getAzureEndpoint());
        assertEquals("gpt-4", config.getAzureChatDeployment());
        assertEquals("text-embedding-ada-002", config.getAzureEmbeddingDeployment());

        // Test logging configuration
        config.setLogRequests(true);
        config.setLogResponses(true);
        assertTrue(config.isLogRequests());
        assertTrue(config.isLogResponses());

        // Test vector store configuration
        config.setVectorstoreBackend("pinecone");
        config.setEmbeddingDimension(1536);
        assertEquals("pinecone", config.getVectorstoreBackend());
        assertEquals(1536, config.getEmbeddingDimension());

        // Test PostgreSQL configuration
        config.setPostgresHost("test-host");
        config.setPostgresPort(5433);
        config.setPostgresDatabase("test-db");
        config.setPostgresUser("test-user");
        config.setPostgresPassword("test-password");
        config.setPostgresTable("test-table");
        
        assertEquals("test-host", config.getPostgresHost());
        assertEquals(5433, config.getPostgresPort());
        assertEquals("test-db", config.getPostgresDatabase());
        assertEquals("test-user", config.getPostgresUser());
        assertEquals("test-password", config.getPostgresPassword());
        assertEquals("test-table", config.getPostgresTable());

        // Test Pinecone configuration
        config.setPineconeApiKey("test-pinecone-key");
        config.setPineconeEnvironment("test-env");
        config.setPineconeProjectId("test-project");
        config.setPineconeIndex("test-index");
        
        assertEquals("test-pinecone-key", config.getPineconeApiKey());
        assertEquals("test-env", config.getPineconeEnvironment());
        assertEquals("test-project", config.getPineconeProjectId());
        assertEquals("test-index", config.getPineconeIndex());

        // Test Weaviate configuration
        config.setWeaviateApiKey("test-weaviate-key");
        config.setWeaviateScheme("https");
        config.setWeaviateHost("test.weaviate.network");
        config.setWeaviateClassName("TestClass");
        
        assertEquals("test-weaviate-key", config.getWeaviateApiKey());
        assertEquals("https", config.getWeaviateScheme());
        assertEquals("test.weaviate.network", config.getWeaviateHost());
        assertEquals("TestClass", config.getWeaviateClassName());
    }

    @Test
    void testLangChain4jConfig_DefaultValues() {
        // Test default values
        assertFalse(config.isLogRequests());
        assertFalse(config.isLogResponses());
        assertEquals("postgres", config.getVectorstoreBackend());
        assertEquals(1536, config.getEmbeddingDimension());
        assertEquals("localhost", config.getPostgresHost());
        assertEquals(5432, config.getPostgresPort());
        assertEquals("silat", config.getPostgresDatabase());
        assertEquals("postgres", config.getPostgresUser());
        assertEquals("embeddings", config.getPostgresTable());
        assertEquals("silat", config.getPineconeIndex());
        assertEquals("https", config.getWeaviateScheme());
        assertEquals("Document", config.getWeaviateClassName());
    }
}