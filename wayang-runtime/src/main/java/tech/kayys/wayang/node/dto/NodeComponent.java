package tech.kayys.wayang.node.dto;

import java.util.Map;

/**
 * 
 * Node component (for composite nodes)
 */
class NodeComponent {
    String id;
    String nodeType;
    Map<String, Object> config;

    NodeComponent(String id, String nodeType, Map<String, Object> config) {
        this.id = id;
        this.nodeType = nodeType;
        this.config = config;
    }
}
