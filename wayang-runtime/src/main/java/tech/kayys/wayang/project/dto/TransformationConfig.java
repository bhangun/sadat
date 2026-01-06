package tech.kayys.wayang.project.dto;

import java.util.List;

/**
 * Transformation Configuration
 */
public record TransformationConfig(List<TransformationStep> steps, String language) {
    // jq, jsonata, javascript, etc.
}
