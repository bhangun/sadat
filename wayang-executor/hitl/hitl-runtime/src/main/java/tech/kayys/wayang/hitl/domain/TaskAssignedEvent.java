package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

record TaskAssignedEvent(
    HumanTaskId taskId,
    TaskAssignment assignment,
    Instant occurredAt
) implements HumanTaskEvent {}