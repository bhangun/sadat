package tech.kayys.silat.ui.schema;

import java.util.List;
import java.util.Map;

/**
 * Validation rule
 */
record ValidationRule(
    String field,
    ValidationType type,
    String message,
    Map<String, Object> params
) {}