package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

record TaskExpiredEvent(
    HumanTaskId taskId,
    Instant occurredAt
) implements HumanTaskEvent {}