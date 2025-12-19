package tech.kayys.wayang.schema.node;

public class PortUI {
    private String widget;
    private Integer order;
    private String helpText;
    private String visibleWhen;
    private boolean editable = true;

    // Getters and setters
    public String getWidget() {
        return widget;
    }

    public void setWidget(String widget) {
        this.widget = widget;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public String getVisibleWhen() {
        return visibleWhen;
    }

    public void setVisibleWhen(String visibleWhen) {
        this.visibleWhen = visibleWhen;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}