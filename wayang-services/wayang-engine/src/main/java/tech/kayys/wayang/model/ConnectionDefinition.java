package tech.kayys.wayang.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.graphql.Ignore;

/**
 * ConnectionDefinition - Edge between nodes
 */
public class ConnectionDefinition {
    public String id;
    public String from; // Source node ID
    public String to; // Target node ID
    public String fromPort; // Source port name
    public String toPort; // Target port name
    public String condition; // CEL expression (optional)
    public ConnectionType type = ConnectionType.DATA;
    @Ignore
    public Map<String, Object> metadata = new HashMap<>();

    public enum ConnectionType {
        DATA, // Normal data flow
        ERROR, // Error handling flow
        CONTROL // Control flow only
    }
}