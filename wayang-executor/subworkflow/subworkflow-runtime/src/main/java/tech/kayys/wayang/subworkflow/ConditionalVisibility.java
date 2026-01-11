package tech.kayys.silat.ui.schema;

import java.util.List;

/**
 * Conditional field visibility
 */
record ConditionalVisibility(
    String dependsOn,      // Field ID this depends on
    String condition,      // JavaScript expression
    List<String> showWhen  // Values that trigger visibility
) {}