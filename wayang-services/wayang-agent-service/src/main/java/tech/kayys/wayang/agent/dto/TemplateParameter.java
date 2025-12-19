package tech.kayys.wayang.agent.dto;

public record TemplateParameter(
        String name,
        String type,
        boolean required,
        String description) {
}
