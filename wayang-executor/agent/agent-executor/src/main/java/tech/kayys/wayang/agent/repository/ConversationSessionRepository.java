package tech.kayys.wayang.agent.repository;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.domain.ConversationSessionEntity;

/**
 * Repository for Conversation Sessions
 */
@ApplicationScoped
public class ConversationSessionRepository
        implements PanacheRepositoryBase<ConversationSessionEntity, String> {

    public Uni<ConversationSessionEntity> findBySessionAndTenant(
            String sessionId,
            String tenantId) {
        return find("sessionId = ?1 and tenantId = ?2", sessionId, tenantId)
                .firstResult();
    }

    public Uni<List<ConversationSessionEntity>> findActiveSessions(String tenantId) {
        return list("tenantId = ?1 and active = true", tenantId);
    }

    public Uni<Long> countActiveSessions(String tenantId) {
        return count("tenantId = ?1 and active = true", tenantId);
    }
}
