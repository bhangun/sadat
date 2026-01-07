package tech.kayys.wayang;

import java.util.Set;

public record ConsumerContext(
    boolean active,
    String consumerId,
    String tenantId,
    String workspaceId,
    String planId,
    Set<String> scopes
) {
    public static ConsumerContext inactive() {
        return new ConsumerContext(false, null, null, null, null, Set.of());
    }

    public static ConsumerContext active(ApiKey k) {
        return new ConsumerContext(
            true,
            k.consumerId,
            k.tenantId,
            k.workspaceId,
            k.planId,
            k.scopes
        );
    }
}
