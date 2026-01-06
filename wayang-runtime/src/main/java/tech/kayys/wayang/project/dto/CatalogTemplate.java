package tech.kayys.wayang.project.dto;

import java.util.List;

import tech.kayys.wayang.domain.CanvasDefinition;

public record CatalogTemplate(
                String id,
                String name,
                String description,
                String category,
                TemplateType templateType,
                CanvasDefinition canvasDefinition,
                List<String> tags,
                String difficulty) {
}
