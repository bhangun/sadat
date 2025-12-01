package tech.kayys.wayang.node.core.validation;

import tech.kayys.wayang.node.core.exception.ValidationException;
import tech.kayys.wayang.node.core.model.NodeContext;
import tech.kayys.wayang.node.core.model.NodeDescriptor;

import java.util.List;

/**
 * Validates node descriptors, inputs, and outputs.
 */
public interface NodeValidator {
    
    /**
     * Validate a node descriptor
     */
    ValidationResult validateDescriptor(NodeDescriptor descriptor);
    
    /**
     * Validate node inputs before execution
     */
    ValidationResult validateInputs(NodeDescriptor descriptor, NodeContext context);
    
    /**
     * Validate node outputs after execution
     */
    ValidationResult validateOutputs(
        NodeDescriptor descriptor, 
        NodeContext context,
        java.util.Map<String, Object> outputs
    );
    
    /**
     * Get validation errors as exception
     */
    default ValidationException toException(ValidationResult result) {
        return new ValidationException(
            "Validation failed: " + result.errors(),
            result
        );
    }
}