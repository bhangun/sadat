package tech.kayys.silat.ui.schema;

/**
 * Configuration section grouping
 */
record ConfigSection(
    String id,
    String label,
    String icon,
    boolean collapsible,
    boolean defaultExpanded,
    List<String> fields
) {}