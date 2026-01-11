package tech.kayys.wayang.mcp.plugin;

import java.util.HashMap;
import java.util.Map;

public class PluginResult {

    private final boolean success;
    private final String message;
    private final Map<String, Object> data = new HashMap<>();
    private final Exception error;

    private PluginResult(boolean success, String message, Exception error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    public static PluginResult success(String message) {
        return new PluginResult(true, message, null);
    }

    public static PluginResult failure(String message, Exception error) {
        return new PluginResult(false, message, error);
    }

    public PluginResult withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    public Exception getError() {
        return error;
    }
}
