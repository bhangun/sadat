package tech.kayys.wayang.automation.dto;

import java.time.Duration;
import java.util.List;

public record ApprovalLevel(
                int levelNumber,
                List<String> approvers,
                String approvalCriteria,
                Duration timeout) {
}
