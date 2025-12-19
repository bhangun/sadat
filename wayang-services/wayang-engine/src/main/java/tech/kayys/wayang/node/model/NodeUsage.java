package tech.kayys.wayang.node.model;

import java.util.List;

/**
 * Node usage information.
 */
@lombok.Data
@lombok.Builder
class NodeUsage {
    private boolean inUse;
    private int activeWorkflows;
    private List<String> workflowIds;

    public static NodeUsage notInUse() {
        return NodeUsage.builder()
                .inUse(false)
                .activeWorkflows(0)
                .workflowIds(List.of())
                .build();
    }
}
