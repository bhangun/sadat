package tech.kayys.wayang.automation.dto;

public record ValidationRule(
                String field,
                String rule,
                String errorMessage) {
}
