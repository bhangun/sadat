package tech.kayys.wayang.node.core.isolation;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.exception.IsolationException;
import tech.kayys.wayang.node.core.model.NodeContext;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.model.SandboxLevel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of isolation management.
 */
@ApplicationScoped
public class DefaultIsolationManager implements IsolationManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultIsolationManager.class);
    
    // Capabilities allowed at each sandbox level
    private static final Map<SandboxLevel, Set<String>> ALLOWED_CAPABILITIES = Map.of(
        SandboxLevel.TRUSTED, Set.of("network", "filesystem", "llm_access", "db_access", "gpu"),
        SandboxLevel.SEMI_TRUSTED, Set.of("network", "llm_access", "db_access"),
        SandboxLevel.UNTRUSTED, Set.of("llm_access")
    );
    
    private final boolean strictMode;
    
    public DefaultIsolationManager(
        @ConfigProperty(name = "wayang.isolation.strict-mode", defaultValue = "true")
        boolean strictMode
    ) {
        this.strictMode = strictMode;
    }
    
    @Override
    public SandboxLevel determineIsolationLevel(NodeDescriptor descriptor) {
        // Use declared sandbox level from descriptor
        SandboxLevel declared = descriptor.sandboxLevel();
        
        // In strict mode, upgrade untrusted capabilities
        if (strictMode && hasUntrustedCapabilities(descriptor)) {
            return SandboxLevel.UNTRUSTED;
        }
        
        return declared;
    }
    
    @Override
    public void applyIsolation(NodeDescriptor descriptor, NodeContext context) 
            throws IsolationException {
        
        SandboxLevel level = descriptor.sandboxLevel();
        
        LOG.debug("Applying isolation level {} for node {}", 
            level, descriptor.getQualifiedId());
        
        // Validate capabilities
        validateCapabilities(descriptor);
        
        // Apply context restrictions
        applyContextRestrictions(context, level);
        
        // Set security metadata
        context.setMetadata("sandbox_level", level.name());
        context.setMetadata("isolation_applied", true);
    }
    
    @Override
    public void validateIsolation(NodeDescriptor descriptor) throws IsolationException {
        SandboxLevel level = descriptor.sandboxLevel();
        List<String> capabilities = descriptor.capabilities();
        
        for (String capability : capabilities) {
            if (!isCapabilityAllowed(capability, level)) {
                throw new IsolationException(
                    String.format(
                        "Capability '%s' not allowed for sandbox level %s in node %s",
                        capability, level, descriptor.getQualifiedId()
                    )
                );
            }
        }
    }
    
    @Override
    public boolean isCapabilityAllowed(String capability, SandboxLevel level) {
        Set<String> allowed = ALLOWED_CAPABILITIES.get(level);
        return allowed != null && allowed.contains(capability);
    }
    
    /**
     * Check if descriptor has untrusted capabilities
     */
    private boolean hasUntrustedCapabilities(NodeDescriptor descriptor) {
        List<String> capabilities = descriptor.capabilities();
        return capabilities.contains("arbitrary_code") ||
               capabilities.contains("system_access") ||
               capabilities.contains("unrestricted_network");
    }
    
    /**
     * Validate that all capabilities are allowed
     */
    private void validateCapabilities(NodeDescriptor descriptor) throws IsolationException {
        SandboxLevel level = descriptor.sandboxLevel();
        
        for (String capability : descriptor.capabilities()) {
            if (!isCapabilityAllowed(capability, level)) {
                throw new IsolationException(
                    "Capability not allowed: " + capability + " at level " + level
                );
            }
        }
    }
    
    /**
     * Apply restrictions to execution context
     */
    private void applyContextRestrictions(NodeContext context, SandboxLevel level) {
        switch (level) {
            case UNTRUSTED -> {
                // Most restrictive
                context.setMetadata("network_access", "none");
                context.setMetadata("filesystem_access", "none");
                context.setMetadata("max_memory_mb", 256);
                context.setMetadata("max_cpu_ms", 30000);
            }
            case SEMI_TRUSTED -> {
                // Moderate restrictions
                context.setMetadata("network_access", "limited");
                context.setMetadata("filesystem_access", "temp_only");
                context.setMetadata("max_memory_mb", 512);
                context.setMetadata("max_cpu_ms", 60000);
            }
            case TRUSTED -> {
                // Minimal restrictions
                context.setMetadata("network_access", "full");
                context.setMetadata("filesystem_access", "controlled");
                context.setMetadata("max_memory_mb", 2048);
                context.setMetadata("max_cpu_ms", 300000);
            }
        }
    }
}