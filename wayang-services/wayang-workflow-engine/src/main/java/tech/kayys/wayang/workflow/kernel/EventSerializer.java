package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.v1.WorkflowEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializes and deserializes workflow events
 */
@ApplicationScoped
public class EventSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(EventSerializer.class);

    private final ObjectMapper objectMapper;

    public EventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // Configure additional modules as needed
    }

    public Uni<String> serialize(WorkflowEvent event) {
        return Uni.createFrom().deferred(() -> {
            try {
                EventDocument doc = toDocument(event);
                String json = objectMapper.writeValueAsString(doc);
                return Uni.createFrom().item(json);
            } catch (JsonProcessingException e) {
                LOG.error("Error serializing event", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<WorkflowEvent> deserialize(String json) {
        return Uni.createFrom().deferred(() -> {
            try {
                EventDocument doc = objectMapper.readValue(json, EventDocument.class);
                WorkflowEvent event = fromDocument(doc);
                return Uni.createFrom().item(event);
            } catch (JsonProcessingException e) {
                LOG.error("Error deserializing event", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<byte[]> serializeToBytes(WorkflowEvent event) {
        return serialize(event)
                .map(json -> json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public Uni<WorkflowEvent> deserializeFromBytes(byte[] bytes) {
        String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        return deserialize(json);
    }

    public Uni<String> serializeData(Map<String, Object> data) {
        return Uni.createFrom().deferred(() -> {
            try {
                String json = objectMapper.writeValueAsString(data);
                return Uni.createFrom().item(json);
            } catch (JsonProcessingException e) {
                LOG.error("Error serializing event data", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<Map<String, Object>> deserializeData(String json) {
        return Uni.createFrom().deferred(() -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(json, Map.class);
                return Uni.createFrom().item(data);
            } catch (JsonProcessingException e) {
                LOG.error("Error deserializing event data", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<WorkflowEvent> cloneEvent(WorkflowEvent event) {
        return serialize(event)
                .flatMap(this::deserialize);
    }

    public Uni<WorkflowEvent> withUpdatedData(WorkflowEvent event, Map<String, Object> newData) {
        return Uni.createFrom().deferred(() -> {
            WorkflowEvent updated = WorkflowEvent.builder()
                    .runId(event.runId())
                    .type(event.type())
                    .timestamp(event.timestamp())
                    .data(new HashMap<>(newData))
                    .build();
            return Uni.createFrom().item(updated);
        });
    }

    private EventDocument toDocument(WorkflowEvent event) {
        EventDocument doc = new EventDocument();
        doc.setRunId(event.runId());
        doc.setType(event.type());
        doc.setTimestamp(event.timestamp());
        doc.setData(event.data());
        doc.setVersion("1.0");
        doc.setSerializedAt(Instant.now());
        return doc;
    }

    private WorkflowEvent fromDocument(EventDocument doc) {
        return WorkflowEvent.builder()
                .runId(doc.getRunId())
                .type(doc.getType())
                .timestamp(doc.getTimestamp())
                .data(doc.getData())
                .build();
    }

    public static class EventDocument {
        private String runId;
        private String type;
        private Instant timestamp;
        private Map<String, Object> data;
        private String version;
        private Instant serializedAt;

        // Getters and setters
        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Instant getSerializedAt() {
            return serializedAt;
        }

        public void setSerializedAt(Instant serializedAt) {
            this.serializedAt = serializedAt;
        }
    }
}