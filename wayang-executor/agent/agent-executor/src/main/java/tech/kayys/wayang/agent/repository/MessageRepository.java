package tech.kayys.wayang.agent.repository;

import java.util.List;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.model.Message;

/**
 * Repository for message persistence
 */
public interface MessageRepository {

    Uni<List<Message>> findBySession(String sessionId, String tenantId);

    Uni<Void> save(String sessionId, String tenantId, List<Message> messages);

    Uni<Void> deleteBySession(String sessionId, String tenantId);

    Uni<List<Message>> search(String sessionId, String tenantId, String query, int limit);

    Uni<Long> count(String sessionId, String tenantId);
}
