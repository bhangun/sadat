package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Aggregate Node Schema
 */
class AggregateNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("AGGREGATE",
            new tech.kayys.silat.ui.schema.UIMetadata("Aggregate", "Control Flow", "layers",
                "#8B5CF6", "#F5F3FF", "#8B5CF6", 140, 60,
                "Wait for multiple branches to complete",
                List.of("aggregate", "join", "merge"), false, null),
            List.of(), List.of(), new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null));
    }
}