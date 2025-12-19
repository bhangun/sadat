package tech.kayys.wayang.workflow.service;

import java.util.Map;

import jakarta.persistence.AttributeConverter;

/**
 * JSONB converter for PostgreSQL.
 */
@jakarta.persistence.Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to JSON", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to map", e);
        }
    }
}