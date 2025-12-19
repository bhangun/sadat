package tech.kayys.wayang.schema.governance;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ContextBinding {
    private String type;
    private String ref;
    private String query;
    private Integer topK = 5;
    private Map<String, Object> filters;
    private Boolean required = true;

    public ContextBinding() {
    }

    public ContextBinding(String type, String ref) {
        this.type = type;
        this.ref = ref;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        List<String> validTypes = Arrays.asList("rag", "memory", "event", "workflow_state");
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("Invalid context binding type: " + type);
        }
        this.type = type;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        if (ref == null || ref.trim().isEmpty()) {
            throw new IllegalArgumentException("Context binding ref cannot be empty");
        }
        this.ref = ref;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        if (topK != null && topK < 0) {
            throw new IllegalArgumentException("TopK cannot be negative");
        }
        this.topK = topK;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
