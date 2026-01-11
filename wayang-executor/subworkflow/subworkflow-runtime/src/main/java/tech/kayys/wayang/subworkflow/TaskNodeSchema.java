package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Task Node Schema
 */
class TaskNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "TASK",
            new tech.kayys.silat.ui.schema.UIMetadata(
                "Task",
                "Core",
                "box",
                "#3B82F6",
                "#EFF6FF",
                "#3B82F6",
                160,
                80,
                "Execute a task or action",
                List.of("task", "action", "execute"),
                false,
                null
            ),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("in", "In", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    true, false, "Trigger", tech.kayys.silat.ui.schema.PortPosition.LEFT, Map.of())
            ),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("success", "Success", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Success", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of()),
                new tech.kayys.silat.ui.schema.UIPort("failure", "Failure", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Failure", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of())
            ),
            new tech.kayys.silat.ui.schema.UIConfiguration(
                List.of(
                    new tech.kayys.silat.ui.schema.ConfigField("executorType", "Executor Type", tech.kayys.silat.ui.schema.FieldType.SELECT,
                        null, true, null, "Type of executor", null,
                        null, List.of(), Map.of()),
                    new tech.kayys.silat.ui.schema.ConfigField("config", "Configuration", tech.kayys.silat.ui.schema.FieldType.JSON,
                        Map.of(), false, "{}", "Task configuration", null,
                        null, null, Map.of("language", "json")),
                    new tech.kayys.silat.ui.schema.ConfigField("timeout", "Timeout (seconds)", tech.kayys.silat.ui.schema.FieldType.NUMBER,
                        30, false, "30", "Execution timeout", null,
                        new tech.kayys.silat.ui.schema.FieldValidation(null, null, 1, 3600, null, null, null),
                        null, Map.of()),
                    new tech.kayys.silat.ui.schema.ConfigField("critical", "Critical", tech.kayys.silat.ui.schema.FieldType.CHECKBOX,
                        false, false, null, "Fail workflow if this fails", null,
                        null, null, Map.of())
                ),
                List.of(
                    new tech.kayys.silat.ui.schema.ConfigSection("basic", "Basic", "settings", false, true,
                        List.of("executorType", "config")),
                    new tech.kayys.silat.ui.schema.ConfigSection("advanced", "Advanced", "sliders", true, false,
                        List.of("timeout", "critical"))
                ),
                Map.of()
            ),
            new tech.kayys.silat.ui.schema.UIValidation(
                List.of(
                    new tech.kayys.silat.ui.schema.ValidationRule("executorType", tech.kayys.silat.ui.schema.ValidationType.REQUIRED,
                        "Executor type is required", Map.of())
                ),
                null
            )
        );
    }
}