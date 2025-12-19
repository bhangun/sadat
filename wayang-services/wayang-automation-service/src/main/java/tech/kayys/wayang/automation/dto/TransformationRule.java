package tech.kayys.wayang.automation.dto;

public record TransformationRule(
                String name,
                String expression,
                String targetField) {
}