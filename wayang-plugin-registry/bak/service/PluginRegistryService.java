package tech.kayys.wayang.plugin.runtime.service;

import java.time.Instant;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import tech.kayys.wayang.plugin.runtime.PluginDescriptor;
import tech.kayys.wayang.plugin.runtime.PluginEntity;
import tech.kayys.wayang.plugin.runtime.PluginStatus;
import tech.kayys.wayang.plugin.runtime.repository.PluginRepository;

/**
 * Plugin Registry Service - Business Logic
 */
@ApplicationScoped
public class PluginRegistryService {

    private static final Logger LOG = Logger.getLogger(PluginRegistryService.class);

    @Inject
    PluginRepository pluginRepository;

    @Inject
    PluginEventEmitter eventEmitter;

    @Inject
    PluginSecurityService securityService;

    /**
     * Register a new plugin
     * Performs validation, security checks, and persistence
     */
    @Transactional
    public Uni<PluginEntity> registerPlugin(PluginDescriptor descriptor, boolean autoApprove) {
        
        // Check if plugin already exists
        return pluginRepository.findByIdAndVersion(descriptor.getId(), descriptor.getVersion())
            .onItem().ifNotNull().failWith(() -> 
                new PluginAlreadyExistsException(descriptor.getId(), descriptor.getVersion())
            )
            .onItem().ifNull().switchTo(() -> {
                // Create new plugin entity
                PluginEntity entity = PluginEntity.fromDescriptor(descriptor);
                entity.setStatus(autoApprove ? PluginStatus.APPROVED : PluginStatus.PENDING);
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());

                // Verify checksum and signature
                return securityService.verifyArtifact(descriptor)
                    .onItem().transformToUni(verified -> {
                        if (!verified) {
                            return Uni.createFrom().failure(
                                new SecurityException("Artifact verification failed")
                            );
                        }

                        // Persist plugin
                        return pluginRepository.persist(entity)
                            .onItem().transformToUni(persisted -> {
                                // Emit registration event
                                return eventEmitter.emitPluginRegistered(persisted)
                                    .replaceWith(persisted);
                            });
                    });
            });
    }

    /**
     * Get plugin by ID and version
     */
    public Uni<PluginEntity> getPlugin(String pluginId, String version) {
        return pluginRepository.findByIdAndVersion(pluginId, version);
    }

    /**
     * Query plugins with filters
     */
    public Uni<PluginQueryResult> queryPlugins(PluginQuery query) {
        return pluginRepository.findWithQuery(query)
            .onItem().transform(plugins -> {
                return PluginQueryResult.builder()
                    .plugins(plugins)
                    .page(query.getPage())
                    .size(query.getSize())
                    .total(plugins.size()) // Should be count query
                    .build();
            });
    }

    /**
     * Update plugin status
     */
    @Transactional
    public Uni<PluginEntity> updateStatus(
            String pluginId, 
            String version, 
            StatusUpdateRequest request) {
        
        return pluginRepository.findByIdAndVersion(pluginId, version)
            .onItem().ifNull().failWith(() -> 
                new PluginNotFoundException(pluginId, version)
            )
            .onItem().transformToUni(plugin -> {
                plugin.setStatus(PluginStatus.valueOf(request.getStatus()));
                plugin.setUpdatedAt(Instant.now());
                
                if (request.getReason() != null) {
                    plugin.setStatusReason(request.getReason());
                }

                return pluginRepository.persist(plugin)
                    .onItem().transformToUni(updated -> 
                        eventEmitter.emitStatusChanged(updated)
                            .replaceWith(updated)
                    );
            });
    }

    /**
     * Deprecate plugin
     */
    @Transactional
    public Uni<Boolean> deprecatePlugin(String pluginId, String version, String reason) {
        return pluginRepository.findByIdAndVersion(pluginId, version)
            .onItem().ifNull().failWith(() -> 
                new PluginNotFoundException(pluginId, version)
            )
            .onItem().transformToUni(plugin -> {
                plugin.setStatus(PluginStatus.DEPRECATED);
                plugin.setStatusReason(reason);
                plugin.setUpdatedAt(Instant.now());

                return pluginRepository.persist(plugin)
                    .replaceWith(true);
            });
    }

    /**
     * Get plugin signature
     */
    public Uni<String> getSignature(String pluginId, String version) {
        return pluginRepository.findByIdAndVersion(pluginId, version)
            .onItem().ifNotNull().transform(plugin -> plugin.getSignature())
            .onItem().ifNull().continueWith((String) null);
    }
}