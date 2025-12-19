package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks data provenance and lineage throughout execution.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProvenanceTracker {

    private static final Logger log = LoggerFactory.getLogger(ProvenanceTracker.class);

    private final Map<String, ProvenanceRecord> records = new ConcurrentHashMap<>();

    /**
     * Record provenance for a node execution.
     */
    public Uni<String> recordProvenance(
            UUID runId,
            String nodeId,
            Map<String, Object> inputs,
            Map<String, Object> outputs,
            String modelVersion) {
        String provenanceId = UUID.randomUUID().toString();

        return Uni.createFrom().item(() -> {
            ProvenanceRecord record = ProvenanceRecord.builder()
                    .provenanceId(provenanceId)
                    .runId(runId)
                    .nodeId(nodeId)
                    .inputs(inputs)
                    .outputs(outputs)
                    .modelVersion(modelVersion)
                    .timestamp(LocalDateTime.now())
                    .build();

            records.put(provenanceId, record);

            log.debug("Recorded provenance: {}", provenanceId);
            return provenanceId;
        });
    }

    /**
     * Get provenance record.
     */
    public Uni<ProvenanceRecord> getProvenance(String provenanceId) {
        return Uni.createFrom().item(() -> records.get(provenanceId));
    }

    public static class ProvenanceRecord {
        private String provenanceId;
        private UUID runId;
        private String nodeId;
        private Map<String, Object> inputs;
        private Map<String, Object> outputs;
        private String modelVersion;
        private LocalDateTime timestamp;

        public ProvenanceRecord() {
        }

        public ProvenanceRecord(String provenanceId, UUID runId, String nodeId, Map<String, Object> inputs,
                Map<String, Object> outputs, String modelVersion, LocalDateTime timestamp) {
            this.provenanceId = provenanceId;
            this.runId = runId;
            this.nodeId = nodeId;
            this.inputs = inputs;
            this.outputs = outputs;
            this.modelVersion = modelVersion;
            this.timestamp = timestamp;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String provenanceId;
            private UUID runId;
            private String nodeId;
            private Map<String, Object> inputs;
            private Map<String, Object> outputs;
            private String modelVersion;
            private LocalDateTime timestamp;

            public Builder provenanceId(String provenanceId) {
                this.provenanceId = provenanceId;
                return this;
            }

            public Builder runId(UUID runId) {
                this.runId = runId;
                return this;
            }

            public Builder nodeId(String nodeId) {
                this.nodeId = nodeId;
                return this;
            }

            public Builder inputs(Map<String, Object> inputs) {
                this.inputs = inputs;
                return this;
            }

            public Builder outputs(Map<String, Object> outputs) {
                this.outputs = outputs;
                return this;
            }

            public Builder modelVersion(String modelVersion) {
                this.modelVersion = modelVersion;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public ProvenanceRecord build() {
                return new ProvenanceRecord(provenanceId, runId, nodeId, inputs, outputs, modelVersion, timestamp);
            }
        }

        public String getProvenanceId() {
            return provenanceId;
        }

        public void setProvenanceId(String provenanceId) {
            this.provenanceId = provenanceId;
        }

        public UUID getRunId() {
            return runId;
        }

        public void setRunId(UUID runId) {
            this.runId = runId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public Map<String, Object> getInputs() {
            return inputs;
        }

        public void setInputs(Map<String, Object> inputs) {
            this.inputs = inputs;
        }

        public Map<String, Object> getOutputs() {
            return outputs;
        }

        public void setOutputs(Map<String, Object> outputs) {
            this.outputs = outputs;
        }

        public String getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}