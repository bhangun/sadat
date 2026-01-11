package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * Approval Node Schema
 */
class ApprovalNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("APPROVAL",
            new tech.kayys.silat.ui.schema.UIMetadata("Approval", "Human", "user-check",
                "#F59E0B", "#FFFBEB", "#F59E0B", 160, 80,
                "Approval workflow with approve/reject",
                List.of("approval", "review", "authorize", "approve"), false, null),
            List.of(), List.of(), new tech.kayys.silat.ui.schema.UIConfiguration(List.of(), List.of(), Map.of()),
            new tech.kayys.silat.ui.schema.UIValidation(List.of(), null));
    }
}