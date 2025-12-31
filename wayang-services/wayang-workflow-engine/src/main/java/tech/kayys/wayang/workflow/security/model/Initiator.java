package tech.kayys.wayang.workflow.security.model;

import java.util.Set;
import lombok.Builder;
import lombok.NonNull;

/**
 * Information about who or what initiated a workflow execution.
 * Decoupled from quarkus-security/OIDC specific classes.
 */
@Builder
public record Initiator(
        @NonNull InitiatorType type,
        @NonNull String userId,
        Set<String> roles) {

    public enum InitiatorType {
        USER, // Human user relying on OIDC
        SYSTEM, // Internal system process (Scheduler, Cron)
        API, // External API Key / Service Account
        WEBHOOK, // External Webhook
        AGENT // AI Agent or Bot
    }
}
