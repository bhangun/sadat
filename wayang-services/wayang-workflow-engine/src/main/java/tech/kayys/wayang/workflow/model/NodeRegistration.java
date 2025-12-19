package tech.kayys.wayang.workflow.model;

import java.util.Map;

import tech.kayys.wayang.common.spi.Node;

/**
 * Node registration metadata
 */
@lombok.Data
@lombok.Builder
public class NodeRegistration {
    private String nodeType;
    private Class<? extends Node> nodeClass;
    private RegistrationType registrationType;
    private String pluginId;
    private String version;
    private java.time.Instant registeredAt;
    private Map<String, Object> metadata;

    public enum RegistrationType {
        BUILT_IN,
        PLUGIN,
        CUSTOM
    }
}
