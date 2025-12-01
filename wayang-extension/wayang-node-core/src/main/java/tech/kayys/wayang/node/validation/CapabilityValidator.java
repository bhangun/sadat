package tech.kayys.wayang.node.core.validation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.isolation.IsolationManager;
import tech.kayys.wayang.node.core.model.NodeContext;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.model.SandboxLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates node capabilities against security policies.
 */
@ApplicationScoped
public class CapabilityValidator implements NodeValidator {
    
    private static final Logger LOG = LoggerFactory.getLogger(CapabilityValidator.class);
    
    // Known capabilities
    private static final Set<String> KNOWN_CAPABILITIES = Set.of(
        "network",
        "filesystem",
        "llm_access",
        "db_access",
        "gpu",
        "tool_execution",
        "memory_access",
        "rag_access"
    );
    
    private final IsolationManager isolationManager;
    
    @Inject
    public CapabilityValidator(IsolationManager isolationManager) {
        this.isolationManager = isolationManager;
    }
    
    @Override
    public ValidationResult validateDescriptor(NodeDescriptor descriptor) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // Validate capabilities
        for (String capability : descriptor.capabilities()) {
            // Check if capability is known
            if (!KNOWN_CAPABILITIES.contains(capability)) {
                warnings.add(new ValidationWarning(
                    "capabilities",
                    "unknown_capability",
                    "Unknown capability: " + capability
                ));
            }
            
            // Check if capability is allowed for sandbox level
            SandboxLevel level = descriptor.sandboxLevel();
            if (!isolationManager.isCapabilityAllowed(capability, level)) {
                errors.add(new ValidationError(
                    "capabilities",
                    "capability_not_allowed",
                    String.format(
                        "Capability '%s' not allowed for sandbox level %s",
                        capability, level
                    ),
                    capability
                ));
            }
        }
        
        // Validate required secrets
        if (descriptor.requiredSecrets() != null && !descriptor.requiredSecrets().isEmpty()) {
            SandboxLevel level = descriptor.sandboxLevel();
            if (level == SandboxLevel.UNTRUSTED) {
                warnings.add(new ValidationWarning(
                    "requiredSecrets",
                    "secrets_in_untrusted",
                    "Untrusted nodes should not require secrets"
                ));
            }
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.failure(errors);
        } else if (!warnings.isEmpty()) {
            return ValidationResult.withWarnings(warnings);
        }
        
        return ValidationResult.success();
    }
    
    @Override
    public ValidationResult validateInputs(NodeDescriptor descriptor, NodeContext context) {
        // No capability-specific input validation needed
        return ValidationResult.success();
    }
    
    @Override
    public ValidationResult validateOutputs(
        NodeDescriptor descriptor,
        NodeContext context,
        java.util.Map<String, Object> outputs
    ) {
        // No capability-specific output validation needed
        return ValidationResult.success();
    }
}