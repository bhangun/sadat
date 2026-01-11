package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

record TaskClaimedEvent(
    HumanTaskId taskId,
    String claimedBy,
    Instant occurredAt
) implements HumanTaskEvent {}