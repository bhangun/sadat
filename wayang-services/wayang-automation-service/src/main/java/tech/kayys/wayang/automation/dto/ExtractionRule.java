package tech.kayys.wayang.automation.dto;

public record ExtractionRule(
                String fieldName,
                String pattern,
                String type) {
}