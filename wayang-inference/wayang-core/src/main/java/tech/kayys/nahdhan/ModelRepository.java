
/**
 * Repository for model artifacts and metadata
 */
public interface ModelRepository {
    
    /**
     * Load model manifest by ID
     */
    Optional<ModelManifest> findById(String modelId, TenantId tenantId);
    
    /**
     * List all models for tenant
     */
    List<ModelManifest> findByTenant(TenantId tenantId, Pageable pageable);
    
    /**
     * Save or update model manifest
     */
    ModelManifest save(ModelManifest manifest);
    
    /**
     * Download model artifact to local cache
     */
    Path downloadArtifact(
        ModelManifest manifest, 
        ModelFormat format
    ) throws ArtifactDownloadException;
    
    /**
     * Check if artifact is cached locally
     */
    boolean isCached(String modelId, ModelFormat format);
    
    /**
     * Evict artifact from local cache
     */
    void evictCache(String modelId, ModelFormat format);
}