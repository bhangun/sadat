package tech.kayys.wayang.schema.node;

public class OutputChannel {
    private String name;
    private String displayName;
    private String description;
    private String type;
    private String condition;
    private PortDescriptor schema;
    private Integer order;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public PortDescriptor getSchema() {
        return schema;
    }

    public void setSchema(PortDescriptor schema) {
        this.schema = schema;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
