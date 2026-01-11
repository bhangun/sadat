package tech.kayys.silat.executor.subworkflow;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input/Output mapper
 */
@ApplicationScoped
class SubWorkflowMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowMapper.class);

    /**
     * Map parent context to child inputs
     */
    public Map<String, Object> mapInputs(
            Map<String, Object> parentContext,
            Map<String, String> mapping) {

        Map<String, Object> childInputs = new HashMap<>();

        if (mapping.isEmpty()) {
            // No mapping: pass through all (except internal fields)
            parentContext.forEach((key, value) -> {
                if (!key.startsWith("_")) {
                    childInputs.put(key, value);
                }
            });
        } else {
            // Apply mapping: childField -> parentField
            mapping.forEach((childField, parentField) -> {
                Object value = resolveValue(parentContext, parentField);
                if (value != null) {
                    childInputs.put(childField, value);
                }
            });
        }

        LOG.debug("Mapped {} parent fields to {} child inputs",
            parentContext.size(), childInputs.size());

        return childInputs;
    }

    /**
     * Map child outputs to parent context
     */
    public Map<String, Object> mapOutputs(
            Map<String, Object> childOutputs,
            Map<String, String> mapping) {

        Map<String, Object> parentOutputs = new HashMap<>();

        if (mapping.isEmpty()) {
            // No mapping: pass through all
            parentOutputs.putAll(childOutputs);
        } else {
            // Apply mapping: parentField -> childField
            mapping.forEach((parentField, childField) -> {
                Object value = resolveValue(childOutputs, childField);
                if (value != null) {
                    parentOutputs.put(parentField, value);
                }
            });
        }

        LOG.debug("Mapped {} child outputs to {} parent fields",
            childOutputs.size(), parentOutputs.size());

        return parentOutputs;
    }

    /**
     * Resolve value from context using path notation
     * Supports: "field", "object.field", "array[0]", "object.array[1].field"
     */
    private Object resolveValue(Map<String, Object> context, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = context;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            // Handle array indexing
            if (part.contains("[")) {
                int bracketIndex = part.indexOf('[');
                String fieldName = part.substring(0, bracketIndex);
                int arrayIndex = Integer.parseInt(
                    part.substring(bracketIndex + 1, part.indexOf(']')));

                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(fieldName);
                }

                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (arrayIndex >= 0 && arrayIndex < list.size()) {
                        current = list.get(arrayIndex);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                // Simple field access
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
            }
        }

        return current;
    }
}