package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Database Query Node Schema
 */
class DatabaseQueryNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("DATABASE_QUERY",
            new tech.kayys.silat.ui.schema.UIMetadata("Database Query", "Data", "database",
                "#6366F1", "#EEF2FF", "#6366F1", 180, 90,
                "Execute database query",
                List.of("database", "sql", "query", "data"), false, null),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("in", "In", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    true, false, "Trigger", tech.kayys.silat.ui.schema.PortPosition.LEFT, Map.of())),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("success", "Success", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Success", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of()),
                new tech.kayys.silat.ui.schema.UIPort("results", "Results", tech.kayys.silat.ui.schema.PortType.DATA, tech.kayys.silat.ui.schema.DataType.ARRAY,
                    false, false, "Query results", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of())
            ),
            new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null)
        );
    }
}