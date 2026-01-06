package tech.kayys.wayang.plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;

/**
 * Runtime schema validator using JSON Schema
 */
@ApplicationScoped
public class SchemaValidator {

    private static final Logger LOG = Logger.getLogger(SchemaValidator.class);

    @Inject
    ObjectMapper objectMapper;

    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    /**
     * Validate data against JSON Schema
     */
    public ValidationResult validate(JsonSchema schema, Map<String, Object> data) {
        if (schema == null) {
            return ValidationResult.success();
        }

        try {
            JsonNode jsonNode = objectMapper.valueToTree(data);
            Set<com.networknt.schema.ValidationMessage> errors = schema.validate(jsonNode);

            if (errors.isEmpty()) {
                return ValidationResult.success();
            } else {
                List<String> errorMessages = errors.stream()
                        .map(com.networknt.schema.ValidationMessage::getMessage)
                        .toList();
                return ValidationResult.failure(String.join(", ", errorMessages));
            }
        } catch (Exception e) {
            LOG.errorf(e, "Schema validation error");
            return ValidationResult.failure("Validation error: " + e.getMessage());
        }
    }

    /**
     * Create JSON Schema from schema string
     */
    public JsonSchema createSchema(String schemaJson) {
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            return schemaFactory.getSchema(schemaNode);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to create schema");
            throw new RuntimeException("Invalid schema", e);
        }
    }
}
