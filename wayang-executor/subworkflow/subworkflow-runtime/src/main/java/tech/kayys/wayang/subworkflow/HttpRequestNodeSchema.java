package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * HTTP Request Node Schema
 */
class HttpRequestNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("HTTP_REQUEST",
            new tech.kayys.silat.ui.schema.UIMetadata("HTTP Request", "Integration", "globe",
                "#10B981", "#F3F4F6", "#10B981", 180, 80,
                "Make HTTP/REST API calls",
                List.of("http", "rest", "api"), false, null),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("in", "In", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    true, false, "Trigger", tech.kayys.silat.ui.schema.PortPosition.LEFT, Map.of())),
            List.of(
                new tech.kayys.silat.ui.schema.UIPort("success", "Success", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Success", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of()),
                new tech.kayys.silat.ui.schema.UIPort("failure", "Failure", tech.kayys.silat.ui.schema.PortType.CONTROL, tech.kayys.silat.ui.schema.DataType.ANY,
                    false, false, "Failure", tech.kayys.silat.ui.schema.PortPosition.RIGHT, Map.of())
            ),
            new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null)
        );
    }
}