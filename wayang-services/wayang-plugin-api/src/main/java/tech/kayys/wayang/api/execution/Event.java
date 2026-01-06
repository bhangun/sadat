package tech.kayys.wayang.api.execution;

import java.util.Map;

public class Event {
    private final String type;
    private final Map<String, Object> data;

    public Event(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
