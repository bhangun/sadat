package tech.kayys.silat.executor.rag.langchain;

import dev.langchain4j.store.embedding.EmbeddingStore;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.executor.rag.domain.LangChain4jConfig;
import tech.kayys.silat.executor.rag.domain.RetrievalConfig;

/**
 * Factory for creating LangChain4j embedding stores
 */
@ApplicationScoped
public class LangChain4jEmbeddingStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LangChain4jEmbeddingStoreFactory.class);

    @Inject
    LangChain4jConfig config;

    public <T> EmbeddingStore<T> getStore(String tenantId, RetrievalConfig retrievalConfig) {
        LOG.debug("Getting embedding store for tenant {}", tenantId);
        
        // Implementation would create store based on configuration
        return null; // Would return actual store implementation
    }
}