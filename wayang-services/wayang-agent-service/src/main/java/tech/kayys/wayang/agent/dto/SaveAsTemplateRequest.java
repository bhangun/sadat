package tech.kayys.wayang.agent.dto;

import java.util.List;

/**
 * Save as template request
 */
public record SaveAsTemplateRequest(
        String name,
        String description,
        String category,
        List<String> useCases,
        String icon) {
}
