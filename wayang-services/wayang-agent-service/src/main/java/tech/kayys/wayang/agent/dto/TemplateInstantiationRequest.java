package tech.kayys.wayang.agent.dto;

import java.util.Map;

/**
 * Template instantiation request
 */
public record TemplateInstantiationRequest(
                String name,
                String tenantId,
                Map<String, Object> parameters) {
}