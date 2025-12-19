package tech.kayys.wayang.sdk.util;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




import tech.kayys.wayang.sdk.dto.WorkflowRunResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Utility for mapping between SDK responses and domain objects
 */
@ApplicationScoped
public class WorkflowResponseMapper {

    @Inject
    ObjectMapper objectMapper;

    /**
     * Extract output data with type conversion
     */
    public <T> T extractOutput(Map<String, Object> output, String key, Class<T> type) {
        Object value = output.get(key);
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, type);
    }

    /**
     * Extract output or return default
     */
    public <T> T extractOutputOrDefault(
        Map<String, Object> output, 
        String key, 
        T defaultValue
    ) {
        Object value = output.get(key);
        if (value == null) {
            return defaultValue;
        }
        @SuppressWarnings("unchecked")
        T result = (T) value;
        return result;
    }
}
