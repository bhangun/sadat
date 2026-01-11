package tech.kayys.wayang.mcp.template;

import java.util.HashMap;
import java.util.Map;

public class CustomTemplate {

    private final String id;
    private final String name;
    private final String fileType;
    private final String content;
    private final Map<String, Object> properties;

    public CustomTemplate(String id, String name, String fileType, String content, Map<String, Object> properties) {
        this.id = id;
        this.name = name;
        this.fileType = fileType;
        this.content = content;
        this.properties = new HashMap<>(properties);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFileType() {
        return fileType;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public String getPropertyAsString(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
}
