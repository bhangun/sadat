package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

record TaskEscalatedEvent(
    HumanTaskId taskId,
    EscalationReason reason,
    String escalatedTo,
    Instant occurredAt
) implements HumanTaskEvent {}