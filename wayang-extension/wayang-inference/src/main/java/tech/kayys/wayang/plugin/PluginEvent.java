package tech.kayys.wayang.plugin;

public record PluginEvent(
    String type,
    Object data,
    String source,
    long timestamp
) {
    public PluginEvent(String type, Object data, String source) {
        this(type, data, source, System.currentTimeMillis());
    }
}