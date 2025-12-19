package tech.kayys.wayang.schema.node;

public class PortDescriptor {
    private String name;
    private String displayName;
    private String description;
    private PortData data;
    private PortUI ui;

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

    public PortData getData() {
        return data;
    }

    public void setData(PortData data) {
        this.data = data;
    }

    public PortUI getUi() {
        return ui;
    }

    public void setUi(PortUI ui) {
        this.ui = ui;
    }
}
