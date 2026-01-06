package tech.kayys.wayang.project.dto;

public record PatternCatalogEntry(
        String id,
        String name,
        String description,
        String category,
        EIPPatternType patternType,
        String documentation) {
}