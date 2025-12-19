package tech.kayys.wayang.workflow.service;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * CEL (Common Expression Language) evaluator.
 */
@ApplicationScoped
class CELEvaluator {

    public <T> T evaluate(String expression, Map<String, Object> variables, Class<T> returnType) {
        // Use CEL library (cel-java or similar)
        // Simplified placeholder
        return null;
    }
}
