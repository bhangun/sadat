package tech.kayys.silat.executor.memory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating appropriate vector store based on configuration
 */
@ApplicationScoped
public class VectorStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(VectorStoreFactory.class);

    @ConfigProperty(name = "silat.memory.store.type", defaultValue = "inmemory")
    String storeType;

    @Inject
    InMemoryVectorStore inMemoryStore;

    @Inject
    PostgresVectorStore postgresStore;

    /**
     * Get configured vector store
     */
    public VectorMemoryStore getVectorStore() {
        LOG.info("Using vector store type: {}", storeType);

        VectorMemoryStore store = switch (storeType.toLowerCase()) {
            case "postgres", "postgresql", "pgvector" -> postgresStore;
            case "inmemory", "memory" -> inMemoryStore;
            default -> {
                LOG.warn("Unknown store type: {}, falling back to in-memory", storeType);
                yield inMemoryStore;
            }
        };

        // Initialize if needed
        if (store instanceof PostgresVectorStore pgStore) {
            pgStore.initialize()
                .subscribe().with(
                    v -> LOG.info("Vector store initialized"),
                    error -> LOG.error("Failed to initialize vector store", error)
                );
        }

        return store;
    }
}