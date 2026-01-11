package tech.kayys.wayang.agent.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.ToolCall;

/**
 * 
 * Helper for JSON serialization/deserialization
 */
@ApplicationScoped
public class JsonMapper {
    private static final Logger LOG = LoggerFactory.getLogger(JsonMapper.class);

    @jakarta.inject.Inject
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public String toJsonArray(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (Exception e) {
            LOG.error("Failed to serialize list", e);
            return "[]";
        }
    }

    public List<String> fromJsonArray(String json) {
        if (json == null || json.isEmpty())
            return List.of();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to deserialize list", e);
            return List.of();
        }
    }

    public String toJsonObject(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map != null ? map : Map.of());
        } catch (Exception e) {
            LOG.error("Failed to serialize map", e);
            return "{}";
        }
    }

    public Map<String, Object> fromJsonObject(String json) {
        if (json == null || json.isEmpty())
            return Map.of();
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            LOG.error("Failed to deserialize map", e);
            return Map.of();
        }
    }

    public String toJsonToolCalls(List<ToolCall> toolCalls) {
        try {
            return objectMapper.writeValueAsString(toolCalls != null ? toolCalls : List.of());
        } catch (Exception e) {
            LOG.error("Failed to serialize tool calls", e);
            return "[]";
        }
    }

    public List<ToolCall> fromJsonToolCalls(String json) {
        if (json == null || json.isEmpty())
            return List.of();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<ToolCall>>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to deserialize tool calls", e);
            return List.of();
        }
    }
}
