package tech.kayys.wayang.automation.dto;

import java.util.List;

public record ETLPipelineRequest(
        String name,
        String tenantId,
        DataSource source,
        List<TransformationRule> transformations,
        List<ValidationRule> validationRules,
        DataDestination destination) {
}