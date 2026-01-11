package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Human Task Node Schema
 */
class HumanTaskNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("HUMAN_TASK",
            new tech.kayys.silat.ui.schema.UIMetadata("Human Task", "Human", "user",
                "#F59E0B", "#FFFBEB", "#F59E0B", 180, 80,
                "Task requiring human input",
                List.of("human", "manual", "approval"), false, null),
            List.of(), List.of(), new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null));
    }
}