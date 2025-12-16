
package tech.kayys.wayang.plugin;

import java.time.Instant;
import java.util.Map;

/**
 * Plugin Event
 */
public class PluginEvent {
    private String eventType;

    private Instant timestamp;

    private String source;
    private Map<String, Object> data;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public static class Builder {
        private String eventType;
        private Instant timestamp;
        private String source;
        private Map<String, Object> data;

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public PluginEvent build() {
            PluginEvent event = new PluginEvent();
            event.setEventType(eventType);
            event.setTimestamp(timestamp);
            event.setSource(source);
            event.setData(data);
            return event;
        }
    }

}