package tech.kayys.wayang.integration.designer;

record AddConnectionRequest(
    String sourceNodeId,
    String targetNodeId,
    String connectionType,
    String condition
) {}