package tech.kayys.silat.ui.schema;

import java.util.Map;

/**
 * Configuration field validation
 */
record FieldValidation(
    Integer minLength,
    Integer maxLength,
    Integer min,
    Integer max,
    String pattern,           // Regex pattern
    String customValidation,  // JavaScript validation function
    List<String> allowedValues
) {}