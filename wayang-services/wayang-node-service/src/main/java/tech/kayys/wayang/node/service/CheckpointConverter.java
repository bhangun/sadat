package tech.kayys.wayang.workflow.service;

import jakarta.persistence.AttributeConverter;

/**
 * Checkpoint data converter.
 */
@jakarta.persistence.Converter
class CheckpointConverter implements AttributeConverter<CheckpointData, String> {

    private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    public String convertToDatabaseColumn(CheckpointData attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert checkpoint to JSON", e);
        }
    }

    @Override
    public CheckpointData convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, CheckpointData.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to checkpoint", e);
        }
    }
}
