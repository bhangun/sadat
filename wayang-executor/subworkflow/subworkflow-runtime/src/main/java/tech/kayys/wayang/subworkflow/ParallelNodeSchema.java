package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Parallel Node Schema
 */
class ParallelNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("PARALLEL",
            new tech.kayys.silat.ui.schema.UIMetadata("Parallel", "Control Flow", "git-merge",
                "#8B5CF6", "#F5F3FF", "#8B5CF6", 140, 60,
                "Execute multiple branches in parallel",
                List.of("parallel", "fork", "concurrent"), false, null),
            List.of(), List.of(), new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null));
    }
}