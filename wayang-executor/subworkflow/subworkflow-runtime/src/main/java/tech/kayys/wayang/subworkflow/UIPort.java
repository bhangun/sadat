package tech.kayys.silat.ui.schema;

import java.util.Map;

/**
 * Port configuration (input/output connection points)
 */
record UIPort(
    String id,
    String label,
    PortType type,           // DATA, CONTROL, EVENT
    DataType dataType,       // STRING, NUMBER, OBJECT, ARRAY, ANY
    boolean required,
    boolean multiple,        // Can connect multiple edges
    String tooltip,
    PortPosition position,   // TOP, RIGHT, BOTTOM, LEFT
    Map<String, Object> customProperties
) {}