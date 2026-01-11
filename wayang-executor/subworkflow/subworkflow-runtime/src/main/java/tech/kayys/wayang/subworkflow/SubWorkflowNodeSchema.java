package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Sub-Workflow Node Schema
 */
class SubWorkflowNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("SUB_WORKFLOW",
            new tech.kayys.silat.ui.schema.UIMetadata("Sub-Workflow", "Workflow", "workflow",
                "#8B5CF6", "#F3F4F6", "#8B5CF6", 200, 100,
                "Execute another workflow as a sub-workflow",
                List.of("workflow", "nested", "composite", "reusable"), false, null),
            List.of(), List.of(), new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null));
    }
}