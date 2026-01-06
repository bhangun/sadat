package tech.kayys.wayang.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Trace Context - OpenTelemetry compatible
 */
public class TraceContext {
    public String traceId;
    public String spanId;
    public String parentSpanId;
    public Map<String, String> baggage = new HashMap<>();
}
