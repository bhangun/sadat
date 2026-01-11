package tech.kayys.silat.ui.schema;

import java.util.List;
import java.util.Map;

/**
 * Configuration field (rendered in properties panel)
 */
record ConfigField(
    String id,
    String label,
    FieldType type,
    Object defaultValue,
    boolean required,
    String placeholder,
    String tooltip,
    String helpText,
    FieldValidation validation,
    List<SelectOption> options,      // For select/radio
    Map<String, Object> fieldConfig  // Type-specific config
) {}