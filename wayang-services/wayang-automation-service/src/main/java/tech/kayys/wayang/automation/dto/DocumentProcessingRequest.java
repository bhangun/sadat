package tech.kayys.wayang.automation.dto;

import java.util.List;

public record DocumentProcessingRequest(
                String name,
                String tenantId,
                List<String> documentTypes,
                List<ExtractionRule> extractionRules,
                double confidenceThreshold) {
}