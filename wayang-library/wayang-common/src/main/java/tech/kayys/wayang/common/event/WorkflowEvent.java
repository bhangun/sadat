package tech.kayys.wayang.common.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@RegisterForReflection
public sealed interface WorkflowEvent permits 
    NodeStartedEvent, NodeCompletedEvent, NodeErrorEvent, 
    PlanCreatedEvent, RunCompletedEvent {
    
    String eventId();
    String runId();
    Instant timestamp();
    String traceId();
}