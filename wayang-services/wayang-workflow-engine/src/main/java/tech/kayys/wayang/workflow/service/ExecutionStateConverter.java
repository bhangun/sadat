package tech.kayys.wayang.workflow.service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tech.kayys.wayang.workflow.model.WorkflowExecutionState;

/**
 * Specialized converter for WorkflowExecutionState
 */
@Converter
class ExecutionStateConverter implements AttributeConverter<WorkflowExecutionState, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(WorkflowExecutionState attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Error converting ExecutionState to JSON", e);
        }
    }

    @Override
    public WorkflowExecutionState convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(dbData,
                    WorkflowExecutionState.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Error converting JSON to ExecutionState", e);
        }
    }
}