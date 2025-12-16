package tech.kayys.wayang.plugin.node;



/**
 * Node Category - For organizing nodes in UI
 */
public class NodeCategory {
    
    
    private String id;
    
    
    private String name;
    
    private String description;
    private String icon;
    private String color;
    private int order = 0;
    
    private String parentCategory;
    
    private boolean collapsible = true;
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return this.color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getParentCategory() {
        return this.parentCategory;
    }

    public void setParentCategory(String parentCategory) {
        this.parentCategory = parentCategory;
    }

    public boolean isCollapsible() {
        return this.collapsible;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String icon;
        private String color;
        private int order = 0;
        private String parentCategory;
        private boolean collapsible = true;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder parentCategory(String parentCategory) {
            this.parentCategory = parentCategory;
            return this;
        }

        public Builder collapsible(boolean collapsible) {
            this.collapsible = collapsible;
            return this;
        }

        public NodeCategory build() {
            NodeCategory nc = new NodeCategory();
            nc.setId(this.id);
            nc.setName(this.name);
            nc.setDescription(this.description);
            nc.setIcon(this.icon);
            nc.setColor(this.color);
            nc.setOrder(this.order);
            nc.setParentCategory(this.parentCategory);
            nc.setCollapsible(this.collapsible);
            return nc;
        }
    }
}