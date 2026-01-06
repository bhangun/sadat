package tech.kayys.wayang.project.dto;

import java.util.List;

import tech.kayys.wayang.domain.CanvasDefinition;

public record CreateTemplateRequest(
                String templateName,
                String description,
                String version,
                TemplateType templateType,
                CanvasDefinition canvasDefinition,
                List<String> tags) {
}