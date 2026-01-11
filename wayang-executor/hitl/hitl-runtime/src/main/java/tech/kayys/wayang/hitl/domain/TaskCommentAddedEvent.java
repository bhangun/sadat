package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

record TaskCommentAddedEvent(
    HumanTaskId taskId,
    String userId,
    String comment,
    Instant occurredAt
) implements HumanTaskEvent {}