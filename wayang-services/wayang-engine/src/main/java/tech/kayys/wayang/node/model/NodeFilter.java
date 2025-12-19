package tech.kayys.wayang.node.model;

import java.util.List;

/**
 * Node filter for searching.
 */
@lombok.Data
@lombok.Builder
class NodeFilter {
    private List<String> capabilities;
    private String status;
    private String category;
    private String sandboxLevel;
    private Boolean builtInOnly;
}
