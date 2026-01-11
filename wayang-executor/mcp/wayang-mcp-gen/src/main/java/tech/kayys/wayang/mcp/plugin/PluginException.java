package tech.kayys.wayang.mcp.plugin;

public class PluginException extends Exception {

    private final String pluginId;
    private final String operation;

    public PluginException(String pluginId, String operation, String message) {
        super(String.format("[%s:%s] %s", pluginId, operation, message));
        this.pluginId = pluginId;
        this.operation = operation;
    }

    public PluginException(String pluginId, String operation, String message, Throwable cause) {
        super(String.format("[%s:%s] %s", pluginId, operation, message), cause);
        this.pluginId = pluginId;
        this.operation = operation;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getOperation() {
        return operation;
    }
}
