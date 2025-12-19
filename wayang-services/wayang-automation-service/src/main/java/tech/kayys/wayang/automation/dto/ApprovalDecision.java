package tech.kayys.wayang.automation.dto;

public record ApprovalDecision(
        boolean approved,
        String reason,
        String comments,
        String notes,
        String decidedBy) {
}