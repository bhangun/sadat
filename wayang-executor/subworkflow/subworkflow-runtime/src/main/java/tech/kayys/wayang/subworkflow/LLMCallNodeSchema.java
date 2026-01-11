package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * LLM Call Node Schema
 */
class LLMCallNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("LLM_CALL",
            new tech.kayys.silat.ui.schema.UIMetadata("LLM Call", "AI/ML", "sparkles",
                "#EC4899", "#FDF2F8", "#EC4899", 200, 100,
                "Call Large Language Model",
                List.of("ai", "llm", "gpt", "claude", "openai", "anthropic"), false, null),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("in", "In", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    true, false, "Trigger", tech.kayys.silat.ui.schema.PortPosition.LEFT, Map.of())),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("success", "Success", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Success", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of()),
                new tech.kayys.silat.ui.schema.UIPort("response", "Response", tech.kayys.silat.ui.schema.PortType.DATA, tech.kayys.silat.ui.schema.DataType.STRING,
                    false, false, "LLM response", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of())
            ),
            new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null)
        );
    }
}