package tech.kayys.wayang.mcp.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateSet {

    private final String id;
    private final String name;
    private final String description;
    private final String version;
    private final Map<String, CustomTemplate> templates = new HashMap<>();

    public TemplateSet(String id, String name, String description, String version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public void addTemplate(CustomTemplate template) {
        templates.put(template.getId(), template);
    }

    public void removeTemplate(String templateId) {
        templates.remove(templateId);
    }

    public List<CustomTemplate> getTemplates() {
        return new ArrayList<>(templates.values());
    }

    public CustomTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public int getTemplateCount() {
        return templates.size();
    }
}
