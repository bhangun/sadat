package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

record TaskDelegatedEvent(
    HumanTaskId taskId,
    String fromUser,
    String toUser,
    String reason,
    Instant occurredAt
) implements HumanTaskEvent {}