package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@Builder
public class NodeDefinition {
    private String id;
    private String name;
    private String type;
    private String implementation;
    private Map<String, Object> config;
    private Map<String, Object> inputMapping;
    private Map<String, Object> outputMapping;
    private Set<String> capabilities;
    private Map<String, Object> metadata;
    private Map<String, Object> uiHints;
    private String description;
}
