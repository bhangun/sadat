
@ApplicationScoped
public class SecurePluginManager implements PluginManager {
    @Inject ArtifactStore artifactStore;
    @Inject PluginScanner pluginScanner;
    @Inject SignatureVerifier signatureVerifier;
    @Inject PluginLoader pluginLoader;
    @Inject GovernanceWorkflow governanceWorkflow;
    @Inject SchemaRegistry schemaRegistry;
    
    @Override
    @Transactional
    public void registerPlugin(PluginDescriptor descriptor, InputStream artifact) {
        // Store artifact
        String artifactId = artifactStore.store(artifact);
        
        // Scan for vulnerabilities
        ScanResult scanResult = pluginScanner.scan(artifactId);
        if (scanResult.hasCriticalVulnerabilities()) {
            throw new PluginSecurityException(
                "Plugin has critical vulnerabilities",
                scanResult.getVulnerabilities()
            );
        }
        
        // Verify signature
        boolean signatureValid = signatureVerifier.verify(
            artifactId,
            descriptor.getSignature()
        );
        if (!signatureValid) {
            throw new InvalidPluginSignatureException();
        }
        
        // Register with governance
        ApprovalRequest approval = governanceWorkflow.submitForApproval(
            descriptor,
            scanResult
        );
        
        // Create plugin entity
        PluginEntity entity = PluginEntity.builder()
            .pluginId(descriptor.getId())
            .name(descriptor.getName())
            .version(descriptor.getVersion())
            .descriptor(descriptor)
            .artifactId(artifactId)
            .status(PluginStatus.PENDING_APPROVAL)
            .approvalRequestId(approval.getId())
            .build();
        
        entityManager.persist(entity);
        
        // Register node schemas
        for (NodeDescriptor nodeDescriptor : descriptor.getNodeDescriptors()) {
            schemaRegistry.registerSchema(nodeDescriptor);
        }
    }
    
    @Override
    public void enablePlugin(String pluginId) {
        PluginEntity entity = getPluginEntity(pluginId);
        
        if (entity.getStatus() != PluginStatus.APPROVED) {
            throw new PluginNotApprovedException(pluginId);
        }
        
        // Load plugin
        pluginLoader.load(entity.getArtifactId(), entity.getDescriptor());
        
        entity.setStatus(PluginStatus.ENABLED);
        entityManager.merge(entity);
    }
    
    @Override
    public void disablePlugin(String pluginId) {
        PluginEntity entity = getPluginEntity(pluginId);
        
        // Unload plugin
        pluginLoader.unload(pluginId);
        
        entity.setStatus(PluginStatus.DISABLED);
        entityManager.merge(entity);
    }
}