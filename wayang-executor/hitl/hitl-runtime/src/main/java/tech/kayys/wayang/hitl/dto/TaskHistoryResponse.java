package tech.kayys.wayang.hitl.dto;

import java.util.List;

record TaskHistoryResponse(
    String taskId,
    List<AuditEntryDto> auditTrail,
    List<AssignmentHistoryDto> assignmentHistory
) {}