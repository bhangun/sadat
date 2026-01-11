package tech.kayys.silat.ui.schema;

/**
 * Select option for dropdowns
 */
record SelectOption(
    String value,
    String label,
    String icon,
    String description
) {}