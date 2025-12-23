package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import java.util.Map;

/**
 * EventDrivenExecutionStrategy - Interface for event-driven workflow execution
 * 
 * This interface allows for workflows that respond to events rather than
 * executing
 * in a predetermined sequence, making the engine suitable for reactive and
 * event-driven use cases.
 */
public interface EventDrivenExecutionStrategy extends WorkflowExecutionStrategy {

    /**
     * Subscribe to events for a workflow
     * 
     * @param workflowId The workflow ID
     * @param eventTypes The event types to subscribe to
     * @return A Uni indicating completion of subscription
     */
    Uni<Void> subscribeToEvents(String workflowId, String[] eventTypes);

    /**
     * Process an incoming event for a workflow
     * 
     * @param workflowId The workflow ID
     * @param eventType  The event type
     * @param eventData  The event data
     * @return A Uni containing the processing result
     */
    Uni<WorkflowRun> processEvent(String workflowId, String eventType, Map<String, Object> eventData);

    /**
     * Check if the workflow is waiting for specific events
     * 
     * @param run The workflow run
     * @return A Uni containing the list of expected events
     */
    Uni<java.util.List<String>> getExpectedEvents(WorkflowRun run);

    /**
     * Resume workflow execution based on event
     * 
     * @param run     The workflow run
     * @param event   The event that triggered resumption
     * @param context The execution context
     * @return A Uni containing the resumed workflow run
     */
    Uni<WorkflowRun> resumeOnEvent(WorkflowRun run, EventPayload event, ExecutionContext context);

    /**
     * Cancel event subscriptions for a workflow
     * 
     * @param workflowId The workflow ID
     * @return A Uni indicating completion
     */
    Uni<Void> cancelSubscriptions(String workflowId);
}

/**
 * EventPayload - Represents an event in the workflow system
 */
class EventPayload {
    private String eventId;
    private String eventType;
    private String source;
    private Map<String, Object> data;
    private long timestamp;
    private String correlationId;
    private String causationId;

    public EventPayload() {
    }

    public EventPayload(String eventId, String eventType, String source,
            Map<String, Object> data, String correlationId, String causationId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.source = source;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.correlationId = correlationId;
        this.causationId = causationId;
    }

    // Getters and setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }
}