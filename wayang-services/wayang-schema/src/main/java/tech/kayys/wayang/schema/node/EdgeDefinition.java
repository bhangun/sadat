package tech.kayys.wayang.schema.node;

import java.util.Map;

public class EdgeDefinition {
    private String id;
    private String from;
    private String to;
    private String fromPort;
    private String toPort;
    private String condition;
    private Map<String, Object> metadata;

    public EdgeDefinition() {
    }

    public EdgeDefinition(String from, String to, String fromPort, String toPort) {
        this.from = from;
        this.to = to;
        this.fromPort = fromPort;
        this.toPort = toPort;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        if (from == null || from.trim().isEmpty()) {
            throw new IllegalArgumentException("Edge source cannot be empty");
        }
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Edge target cannot be empty");
        }
        this.to = to;
    }

    public String getFromPort() {
        return fromPort;
    }

    public void setFromPort(String fromPort) {
        if (fromPort == null || fromPort.trim().isEmpty()) {
            throw new IllegalArgumentException("Source port cannot be empty");
        }
        this.fromPort = fromPort;
    }

    public String getToPort() {
        return toPort;
    }

    public void setToPort(String toPort) {
        if (toPort == null || toPort.trim().isEmpty()) {
            throw new IllegalArgumentException("Target port cannot be empty");
        }
        this.toPort = toPort;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
