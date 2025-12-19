package tech.kayys.wayang.workflow.service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tech.kayys.wayang.schema.execution.ErrorPayload;

/**
 * Specialized converter for ErrorPayload
 */
@Converter
class ErrorPayloadConverter implements AttributeConverter<ErrorPayload, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(ErrorPayload attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Error converting ErrorPayload to JSON", e);
        }
    }

    @Override
    public ErrorPayload convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(dbData,
                    ErrorPayload.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Error converting JSON to ErrorPayload", e);
        }
    }
}