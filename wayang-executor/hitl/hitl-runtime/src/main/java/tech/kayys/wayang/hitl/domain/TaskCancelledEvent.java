package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

record TaskCancelledEvent(
    HumanTaskId taskId,
    String cancelledBy,
    String reason,
    Instant occurredAt
) implements HumanTaskEvent {}