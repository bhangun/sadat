package tech.kayys.wayang.hitl.domain;

import java.time.Instant;
import java.util.Map;

record TaskCreatedEvent(
    HumanTaskId taskId,
    String workflowRunId,
    String nodeId,
    Instant occurredAt
) implements HumanTaskEvent {}