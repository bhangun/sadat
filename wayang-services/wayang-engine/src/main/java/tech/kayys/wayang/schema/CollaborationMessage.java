package tech.kayys.wayang.schema;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

import jakarta.validation.constraints.NotNull;
import tech.kayys.wayang.schema.MessageType;
import tech.kayys.wayang.utils.JsonUtils;

/**
 * CollaborationMessage - Message sent by client
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollaborationMessage {

    @NotNull
    private MessageType type;

    @NotNull
    private Map<String, Object> payload = new HashMap<>();

    private String messageId;
    private Instant timestamp;

    // Getters and setters...
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> clazz) {
        return JsonUtils.convertValue(payload, clazz);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
