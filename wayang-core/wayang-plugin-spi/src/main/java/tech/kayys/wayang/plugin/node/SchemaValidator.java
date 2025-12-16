package tech.kayys.wayang.plugin.node;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.PortDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates data against JSON schemas.
 * 
 * Caches compiled schemas for performance.
 */
@ApplicationScoped
public class SchemaValidator implements NodeValidator {
    
    private static final Logger LOG = LoggerFactory.getLogger(SchemaValidator.class);
    
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final Map<String, JsonSchema> schemaCache;
    
    @Inject
    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.schemaCache = new ConcurrentHashMap<>();
    }
    
    @Override
    public ValidationResult validateDescriptor(NodeDescriptor descriptor) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Validate descriptor structure
        if (descriptor.id() == null || descriptor.id().isBlank()) {
            errors.add(new ValidationError("id", "required", "Node ID is required", null));
        }
        
        if (descriptor.name() == null || descriptor.name().isBlank()) {
            errors.add(new ValidationError("name", "required", "Node name is required", null));
        }
        
        if (descriptor.version() == null || descriptor.version().isBlank()) {
            errors.add(new ValidationError("version", "required", "Version is required", null));
        }
        
        // Validate inputs
        for (PortDescriptor input : descriptor.inputs()) {
            if (input.required() && input.defaultValue() == null) {
                // Required input without default is valid
            }
        }
        
        // Validate implementation
        if (descriptor.implementation() == null) {
            errors.add(new ValidationError(
                "implementation", 
                "required", 
                "Implementation descriptor is required", 
                null
            ));
        }
        
        return errors.isEmpty() ? 
            ValidationResult.success() : 
            ValidationResult.failure(errors);
    }
    
    @Override
    public ValidationResult validateInputs(NodeDescriptor descriptor, NodeContext context) {
        List<ValidationError> errors = new ArrayList<>();
        
        for (PortDescriptor input : descriptor.inputs()) {
            String inputName = input.name();
            
            // Check required inputs
            if (input.required()) {
                Object value = context.getVariable(inputName, Object.class).orElse(null);
                if (value == null && input.defaultValue() == null) {
                    errors.add(new ValidationError(
                        inputName,
                        "required",
                        "Required input '" + inputName + "' is missing",
                        null
                    ));
                    continue;
                }
            }
            
            // Validate against schema if present
            if (!input.schema().isEmpty()) {
                Object value = context.getVariable(inputName, Object.class).orElse(input.defaultValue());
                if (value != null) {
                    List<ValidationError> schemaErrors = validateAgainstSchema(
                        inputName, 
                        value, 
                        input.schema()
                    );
                    errors.addAll(schemaErrors);
                }
            }
        }
        
        return errors.isEmpty() ? 
            ValidationResult.success() : 
            ValidationResult.failure(errors);
    }
    
    @Override
    public ValidationResult validateOutputs(
        NodeDescriptor descriptor,
        NodeContext context,
        Map<String, Object> outputs
    ) {
        List<ValidationError> errors = new ArrayList<>();
        
        for (PortDescriptor output : descriptor.outputs()) {
            String outputName = output.name();
            
            // Check required outputs
            if (output.required() && !outputs.containsKey(outputName)) {
                errors.add(new ValidationError(
                    outputName,
                    "required",
                    "Required output '" + outputName + "' is missing",
                    null
                ));
                continue;
            }
            
            // Validate against schema if present
            if (!output.schema().isEmpty() && outputs.containsKey(outputName)) {
                Object value = outputs.get(outputName);
                List<ValidationError> schemaErrors = validateAgainstSchema(
                    outputName,
                    value,
                    output.schema()
                );
                errors.addAll(schemaErrors);
            }
        }
        
        return errors.isEmpty() ? 
            ValidationResult.success() : 
            ValidationResult.failure(errors);
    }
    
    /**
     * Validate value against JSON schema
     */
    private List<ValidationError> validateAgainstSchema(
        String fieldName,
        Object value,
        Map<String, Object> schemaMap
    ) {
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // Convert schema map to JsonNode
            JsonNode schemaNode = objectMapper.valueToTree(schemaMap);
            
            // Get or compile schema
            String schemaKey = schemaNode.toString();
            JsonSchema schema = schemaCache.computeIfAbsent(schemaKey, 
                key -> schemaFactory.getSchema(schemaNode));
            
            // Convert value to JsonNode
            JsonNode valueNode = objectMapper.valueToTree(value);
            
            // Validate
            Set<ValidationMessage> messages = schema.validate(valueNode);
            
            // Convert to ValidationErrors
            for (ValidationMessage msg : messages) {
                errors.add(new ValidationError(
                    fieldName,
                    msg.getType(),
                    msg.getMessage(),
                    value
                ));
            }
            
        } catch (Exception e) {
            LOG.error("Schema validation error for field: " + fieldName, e);
            errors.add(new ValidationError(
                fieldName,
                "schema_error",
                "Failed to validate schema: " + e.getMessage(),
                value
            ));
        }
        
        return errors;
    }
    
    /**
     * Clear schema cache
     */
    public void clearCache() {
        schemaCache.clear();
        LOG.info("Cleared schema cache");
    }
}
