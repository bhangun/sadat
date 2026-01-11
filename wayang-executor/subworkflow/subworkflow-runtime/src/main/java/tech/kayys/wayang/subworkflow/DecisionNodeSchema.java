package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Decision Node Schema
 */
class DecisionNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "DECISION",
            new tech.kayys.silat.ui.schema.UIMetadata(
                "Decision",
                "Control Flow",
                "git-branch",
                "#F59E0B",
                "#FFFBEB",
                "#F59E0B",
                140,
                60,
                "Conditional branching based on expressions",
                List.of("decision", "if", "condition", "branch"),
                false,
                null
            ),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("in", "In", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    true, false, "Input", tech.kayys.silat.ui.schema.PortPosition.LEFT, Map.of())
            ),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("true", "True", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Condition true", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of()),
                new tech.kayys.silat.ui.schema.UIPort("false", "False", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Condition false", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of())
            ),
            new tech.kayys.silat.ui.schema.UIConfiguration(
                List.of(
                    new tech.kayys.silat.ui.schema.ConfigField("condition", "Condition", tech.kayys.silat.ui.schema.FieldType.EXPRESSION,
                        null, true, "amount > 1000", "Boolean expression",
                        "Use JavaScript expression syntax",
                        null, null, Map.of("language", "javascript")),
                    new tech.kayys.silat.ui.schema.ConfigField("description", "Description", tech.kayys.silat.ui.schema.FieldType.TEXTAREA,
                        null, false, "Describe the decision logic",
                        "Human-readable description", null, null, Map.of())
                ),
                List.of(),
                Map.of()
            ),
            new tech.kayys.silat.ui.schema.UIValidation(
                List.of(
                    new tech.kayys.silat.ui.schema.ValidationRule("condition", tech.kayys.silat.ui.schema.ValidationType.REQUIRED,
                        "Condition expression is required", Map.of())
                ),
                null
            )
        );
    }
}