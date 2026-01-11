package tech.kayys.silat.ui.schema;

import java.util.List;

/**
 * Complete UI metadata for visual designer
 */
record UIMetadata(
    String displayName,
    String category,          // "Task", "Control Flow", "Integration", "AI", etc.
    String icon,              // Icon identifier (lucide, fontawesome, etc.)
    String color,             // Primary color (hex)
    String backgroundColor,   // Background color
    String borderColor,       // Border color
    int defaultWidth,         // Default node width
    int defaultHeight,        // Default node height
    String description,       // User-facing description
    List<String> tags,        // Search tags
    boolean deprecated,       // Whether node type is deprecated
    String replacedBy         // If deprecated, what replaces it
) {}