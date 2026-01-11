package tech.kayys.wayang.integration.designer;

record ValidationIssue(
    String severity,
    String code,
    String message,
    String nodeId
) {}