package tech.kayys.silat.ui.schema;

import java.util.List;
import java.util.Map;

/**
 * Node configuration form schema (for properties panel)
 */
record UIConfiguration(
    List<ConfigField> fields,
    List<ConfigSection> sections,
    Map<String, ConditionalVisibility> conditionalFields
) {}