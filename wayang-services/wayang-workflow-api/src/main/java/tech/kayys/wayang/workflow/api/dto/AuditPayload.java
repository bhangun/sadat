package tech.kayys.wayang.workflow.api.dto;

/**
 * Audit payload schema.
 */

import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import tech.kayys.wayang.schema.execution.ErrorPayload;

/***
 * Standardized audit payload for tamper-proof execution logs.
 * Provides chain-of-custody tracking for compliance and debugging.
 * 
 * Features:
 * - Immutable event recording
 * - Actor tracking (system/human/agent)
 * - Context snapshots
 * - Cryptographic hashing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditPayload {

    public static final String Level = null;
    private LocalDateTime timestamp = LocalDateTime.now();
    private UUID runId;
    private String nodeId;
    private Actor actor;
    private String event;
    private AuditLevel level;
    private List<String> tags = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private Map<String, Object> contextSnapshot;
    private String hash;
    private String previousHash;

    public AuditPayload() {
    }

    public AuditPayload(LocalDateTime timestamp, UUID runId, String nodeId, Actor actor, String event, AuditLevel level,
            List<String> tags, Map<String, Object> metadata, Map<String, Object> contextSnapshot, String hash,
            String previousHash) {
        this.timestamp = timestamp;
        this.runId = runId;
        this.nodeId = nodeId;
        this.actor = actor;
        this.event = event;
        this.level = level;
        this.tags = tags;
        this.metadata = metadata;
        this.contextSnapshot = contextSnapshot;
        this.hash = hash;
        this.previousHash = previousHash;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime timestamp = LocalDateTime.now();
        private UUID runId;
        private String nodeId;
        private Actor actor;
        private String event;
        private AuditLevel level;
        private List<String> tags = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();
        private Map<String, Object> contextSnapshot;
        private String hash;
        private String previousHash;

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
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

        public Builder actor(Actor actor) {
            this.actor = actor;
            return this;
        }

        public Builder event(String event) {
            this.event = event;
            return this;
        }

        public Builder level(AuditLevel level) {
            this.level = level;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder contextSnapshot(Map<String, Object> contextSnapshot) {
            this.contextSnapshot = contextSnapshot;
            return this;
        }

        public Builder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public Builder previousHash(String previousHash) {
            this.previousHash = previousHash;
            return this;
        }

        public AuditPayload build() {
            return new AuditPayload(timestamp, runId, nodeId, actor, event, level, tags, metadata, contextSnapshot,
                    hash, previousHash);
        }

        public Builder systemActor() {
            // TODO Auto-generated method stub
            this.actor = Actor.builder().type(ActorType.SYSTEM).id("system").role("system").build();
            return this;
        }
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public Actor getActor() {
        return actor;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public AuditLevel getLevel() {
        return level;
    }

    public void setLevel(AuditLevel level) {
        this.level = level;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getContextSnapshot() {
        return contextSnapshot;
    }

    public void setContextSnapshot(Map<String, Object> contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public enum Level {
        INFO, DEBUG
    }

    public static class Actor {
        private ActorType type;
        private String id;
        private String role;

        public Actor() {
        }

        public Actor(ActorType type, String id, String role) {
            this.type = type;
            this.id = id;
            this.role = role;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ActorType type;
            private String id;
            private String role;

            public Builder type(ActorType type) {
                this.type = type;
                return this;
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder role(String role) {
                this.role = role;
                return this;
            }

            public Actor build() {
                return new Actor(type, id, role);
            }
        }

        public ActorType getType() {
            return type;
        }

        public void setType(ActorType type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public enum ActorType {
        SYSTEM,
        HUMAN,
        AGENT
    }

    public static class Events {
        public static final String NODE_START = "NODE_START";
        public static final String NODE_SUCCESS = "NODE_SUCCESS";
        public static final String NODE_ERROR = "NODE_ERROR";
    }

    public enum AuditLevel {
        INFO,
        WARN,
        ERROR,
        CRITICAL
    }

    public static AuditPayload createNodeStartEvent(UUID runId, String nodeId) {
        return AuditPayload.builder()
                .runId(runId)
                .nodeId(nodeId)
                .event("NODE_START")
                .level(AuditLevel.INFO)
                .actor(Actor.builder().type(ActorType.SYSTEM).id("orchestrator").build())
                .build();
    }

    public static AuditPayload createNodeErrorEvent(UUID runId, String nodeId, ErrorPayload error) {
        return AuditPayload.builder()
                .runId(runId)
                .nodeId(nodeId)
                .event("NODE_ERROR")
                .level(AuditLevel.ERROR)
                .actor(Actor.builder().type(ActorType.SYSTEM).id("orchestrator").build())
                .metadata(Map.of("error", error))
                .build();
    }

    public static AuditPayload createHITLEvent(UUID runId, String nodeId, String operatorId, String action) {
        return AuditPayload.builder()
                .runId(runId)
                .nodeId(nodeId)
                .event("HITL_COMPLETED")
                .level(AuditLevel.INFO)
                .actor(Actor.builder().type(ActorType.HUMAN).id(operatorId).role("operator").build())
                .metadata(Map.of("action", action))
                .build();
    }
}
