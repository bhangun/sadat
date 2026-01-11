package tech.kayys.silat.ui.schema;

import java.util.List;
import java.util.Map;

/**
 * Validation rules for node configuration
 */
record UIValidation(
    List<ValidationRule> rules,
    String customValidator  // JavaScript validation function
) {}