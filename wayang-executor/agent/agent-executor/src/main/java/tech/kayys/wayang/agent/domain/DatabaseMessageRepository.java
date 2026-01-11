package tech.kayys.wayang.agent.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.ToolCall;
import tech.kayys.wayang.agent.repository.ConversationMessageRepository;
import tech.kayys.wayang.agent.repository.ConversationSessionRepository;
import tech.kayys.wayang.agent.repository.MessageRepository;
import tech.kayys.wayang.agent.service.JsonMapper;

/**
 * Production MessageRepository using database
 */
@ApplicationScoped
public class DatabaseMessageRepository implements MessageRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseMessageRepository.class);

    @jakarta.inject.Inject
    ConversationMessageRepository messageRepo;

    @jakarta.inject.Inject
    ConversationSessionRepository sessionRepo;

    @jakarta.inject.Inject
    JsonMapper jsonMapper;

    @Override
    public Uni<List<Message>> findBySession(String sessionId, String tenantId) {
        LOG.info("findBySession: sessionId={}, tenantId={}", sessionId, tenantId);
        return messageRepo.findBySession(sessionId, tenantId)
                .map(entities -> entities.stream()
                        .map(this::toMessage)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Void> save(String sessionId, String tenantId, List<Message> messages) {
        // Ensure session exists
        return sessionRepo.findBySessionAndTenant(sessionId, tenantId)
                .flatMap(session -> {
                    if (session == null) {
                        session = createNewSession(sessionId, tenantId);
                        return sessionRepo.persist(session)
                                .flatMap(s -> saveMessages(s, messages));
                    } else {
                        return saveMessages(session, messages);
                    }
                });
    }

    private Uni<Void> saveMessages(
            ConversationSessionEntity session,
            List<Message> messages) {

        return messageRepo.getNextSequenceNumber(
                session.getSessionId(),
                session.getTenantId())
                .flatMap(startSeq -> {
                    List<ConversationMessageEntity> entities = new ArrayList<>();

                    for (int i = 0; i < messages.size(); i++) {
                        Message msg = messages.get(i);
                        ConversationMessageEntity entity = toEntity(
                                msg,
                                session.getSessionId(),
                                session.getTenantId(),
                                startSeq + i);
                        entities.add(entity);
                    }

                    return messageRepo.persist(entities)
                            .flatMap(v -> updateSessionStats(session, messages.size()))
                            .replaceWithVoid();
                });
    }

    @Override
    public Uni<Void> deleteBySession(String sessionId, String tenantId) {
        return messageRepo.deleteBySession(sessionId, tenantId)
                .flatMap(deleted -> sessionRepo.findBySessionAndTenant(sessionId, tenantId)
                        .flatMap(session -> {
                            if (session != null) {
                                session.setActive(false);
                                session.setClosedAt(Instant.now());
                                return sessionRepo.persist(session);
                            }
                            return Uni.createFrom().voidItem();
                        }))
                .replaceWithVoid();
    }

    @Override
    public Uni<List<Message>> search(
            String sessionId,
            String tenantId,
            String query,
            int limit) {
        // Simple content search
        // In production, use full-text search or vector search
        return messageRepo.findBySession(sessionId, tenantId)
                .map(entities -> entities.stream()
                        .filter(e -> e.getContent() != null &&
                                e.getContent().toLowerCase().contains(query.toLowerCase()))
                        .limit(limit)
                        .map(this::toMessage)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Long> count(String sessionId, String tenantId) {
        return messageRepo.count("sessionId = ?1 and tenantId = ?2", sessionId, tenantId);
    }

    private ConversationSessionEntity createNewSession(String sessionId, String tenantId) {
        ConversationSessionEntity session = new ConversationSessionEntity();
        session.setSessionId(sessionId);
        session.setTenantId(tenantId);
        session.setActive(true);
        return session;
    }

    private Uni<Void> updateSessionStats(ConversationSessionEntity session, int messageCount) {
        session.setMessageCount(session.getMessageCount() + messageCount);
        return sessionRepo.persist(session).replaceWithVoid();
    }

    private Message toMessage(ConversationMessageEntity entity) {
        List<ToolCall> toolCalls = entity.getToolCalls() != null ? jsonMapper.fromJsonToolCalls(entity.getToolCalls())
                : null;

        return new Message(
                entity.getRole(),
                entity.getContent(),
                toolCalls,
                entity.getToolCallId(),
                entity.getTimestamp());
    }

    private ConversationMessageEntity toEntity(
            Message msg,
            String sessionId,
            String tenantId,
            int sequenceNumber) {

        ConversationMessageEntity entity = new ConversationMessageEntity();
        entity.setSessionId(sessionId);
        entity.setTenantId(tenantId);
        entity.setSequenceNumber(sequenceNumber);
        entity.setRole(msg.role());
        entity.setContent(msg.content());
        entity.setToolCalls(msg.hasToolCalls() ? jsonMapper.toJsonToolCalls(msg.toolCalls()) : null);
        entity.setToolCallId(msg.toolCallId());
        entity.setTimestamp(msg.timestamp());

        return entity;
    }

}
