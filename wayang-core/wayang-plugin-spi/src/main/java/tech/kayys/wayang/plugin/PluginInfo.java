package tech.kayys.wayang.plugin;

/**
 * Plugin Info
 */
public class PluginInfo {
    private String id;
    private String version;
    private String name;
    private PluginStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public static class Builder {
        private String id;
        private String version;
        private String name;
        private PluginStatus status;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setStatus(PluginStatus status) {
            this.status = status;
            return this;
        }

        public PluginInfo build() {
            PluginInfo pluginInfo = new PluginInfo();
            pluginInfo.setId(id);
            pluginInfo.setVersion(version);
            pluginInfo.setName(name);
            pluginInfo.setStatus(status);
            return pluginInfo;
        }
    }
}
