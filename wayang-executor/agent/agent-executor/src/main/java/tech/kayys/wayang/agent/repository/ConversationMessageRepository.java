package tech.kayys.wayang.agent.repository;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.domain.ConversationMessageEntity;

/**
 * Repository for Conversation Messages
 */
@ApplicationScoped
public class ConversationMessageRepository
                implements PanacheRepositoryBase<ConversationMessageEntity, String> {

        public Uni<List<ConversationMessageEntity>> findBySession(
                        String sessionId,
                        String tenantId) {
                return list("sessionId = ?1 and tenantId = ?2 order by sequenceNumber",
                                sessionId, tenantId);
        }

        public Uni<List<ConversationMessageEntity>> findBySessionWithLimit(
                        String sessionId,
                        String tenantId,
                        int limit) {
                return find(
                                "sessionId = ?1 and tenantId = ?2 order by sequenceNumber desc",
                                sessionId, tenantId).page(0, limit).list();
        }

        public Uni<Integer> getNextSequenceNumber(String sessionId, String tenantId) {
                return find("sessionId = ?1 and tenantId = ?2", sessionId, tenantId)
                                .count()
                                .map(count -> count.intValue());
        }

        public Uni<Boolean> deleteBySession(String sessionId, String tenantId) {
                return delete("sessionId = ?1 and tenantId = ?2", sessionId, tenantId)
                                .map(count -> count > 0);
        }
}
