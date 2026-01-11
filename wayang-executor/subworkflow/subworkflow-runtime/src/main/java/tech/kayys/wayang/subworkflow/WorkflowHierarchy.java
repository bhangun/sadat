package tech.kayys.silat.api.subworkflow;

import java.util.List;

record WorkflowHierarchy(
    WorkflowHierarchyNode root,
    int totalNodes,
    int maxDepth
) {}