package tech.kayys.wayang.automation.dto;

import java.util.List;
import java.util.Map;

public record ApprovalWorkflowRequest(
                String name,
                String tenantId,
                List<ApprovalLevel> approvalLevels,
                boolean allowRejection,
                Map<String, Object> metadata) {
}
