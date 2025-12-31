package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DefaultEventDrivenExecutionStrategy - Concrete implementation of event-driven
 * workflow execution
 *
 * This strategy executes workflows in response to events. Nodes subscribe to
 * specific events
 * and execute when those events are triggered. Useful for reactive and
 * asynchronous workflows.
 */
@ApplicationScoped
public class DefaultEventDrivenExecutionStrategy implements EventDrivenExecutionStrategy {

    private static final Logger LOG = Logger.getLogger(DefaultEventDrivenExecutionStrategy.class);

    @Inject
    StateStore stateStore;

    // Track event subscriptions per workflow
    private final Map<String, Set<String>> workflowSubscriptions = new ConcurrentHashMap<>();

    // Track pending events per workflow run
    private final Map<String, Queue<EventPayload>> pendingEvents = new ConcurrentHashMap<>();

    @Override
    public String getStrategyType() {
        return "EVENT_DRIVEN";
    }

    @Override
    public boolean canHandle(WorkflowDefinition workflow) {
        // Check if workflow has event-driven metadata
        if (workflow.getMetadata() != null) {
            return "EVENT_DRIVEN".equals(workflow.getMetadata().get("executionStrategy"));
        }
        return false;
    }

    @Override
    public Uni<WorkflowRun> execute(WorkflowDefinition workflow, Map<String, Object> inputs, ExecutionContext context) {
        LOG.infof("Starting event-driven execution for workflow: %s", workflow.getId());

        // Subscribe to initial events
        String[] eventTypes = determineRequiredEvents(workflow);

        return subscribeToEvents(workflow.getId().getValue(), eventTypes)
                .onItem().transformToUni(v -> {
                    // Mark workflow as waiting for events
                    context.getWorkflowRun().suspend("Awaiting events: " + Arrays.toString(eventTypes));
                    return stateStore.save(context.getWorkflowRun());
                });
    }

    @Override
    public Uni<Void> subscribeToEvents(String workflowId, String[] eventTypes) {
        LOG.infof("Subscribing workflow %s to events: %s", workflowId, Arrays.toString(eventTypes));

        Set<String> events = workflowSubscriptions.computeIfAbsent(workflowId, k -> ConcurrentHashMap.newKeySet());
        events.addAll(Arrays.asList(eventTypes));

        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<WorkflowRun> processEvent(String workflowId, String eventType, Map<String, Object> eventData) {
        LOG.infof("Processing event %s for workflow: %s", eventType, workflowId);

        // Create event payload
        EventPayload event = new EventPayload(
                UUID.randomUUID().toString(),
                eventType,
                workflowId,
                eventData,
                UUID.randomUUID().toString(),
                null);

        // Queue the event for processing
        pendingEvents.computeIfAbsent(workflowId, k -> new LinkedList<>()).offer(event);

        // For now, return a placeholder run
        // In a real implementation, this would trigger workflow execution
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Event processing requires run context"));
    }

    @Override
    public Uni<List<String>> getExpectedEvents(WorkflowRun run) {
        Set<String> events = workflowSubscriptions.get(run.getWorkflowId());
        if (events == null) {
            return Uni.createFrom().item(Collections.emptyList());
        }
        return Uni.createFrom().item(new ArrayList<>(events));
    }

    @Override
    public Uni<WorkflowRun> resumeOnEvent(WorkflowRun run, EventPayload event, ExecutionContext context) {
        LOG.infof("Resuming workflow %s on event: %s", run.getRunId(), event.getEventType());

        // Update context with event data
        if (event.getData() != null) {
            event.getData().forEach((key, value) -> context.setVariable("event." + key, value));
        }
        context.setVariable("event.type", event.getEventType());
        context.setVariable("event.id", event.getEventId());

        // Resume the workflow run
        run.setStatus(tech.kayys.wayang.workflow.api.model.RunStatus.RUNNING);

        return stateStore.save(run);
    }

    @Override
    public Uni<Void> cancelSubscriptions(String workflowId) {
        LOG.infof("Canceling event subscriptions for workflow: %s", workflowId);

        workflowSubscriptions.remove(workflowId);
        pendingEvents.remove(workflowId);

        return Uni.createFrom().voidItem();
    }

    /**
     * Determine which events are required for this workflow
     */
    private String[] determineRequiredEvents(WorkflowDefinition workflow) {
        // Extract event types from workflow metadata or node configurations
        if (workflow.getMetadata() != null && workflow.getMetadata().containsKey("requiredEvents")) {
            String eventsStr = (String) workflow.getMetadata().get("requiredEvents");
            return eventsStr.split(",");
        }

        // Extract from nodes
        Set<String> events = new HashSet<>();
        workflow.getNodes().forEach(node -> {
            if (node.getProperties() != null) {
                node.getProperties().stream()
                        .filter(prop -> "eventType".equals(prop.getName()))
                        .forEach(prop -> {
                            if (prop.getDefault() != null) {
                                events.add(String.valueOf(prop.getDefault()));
                            }
                        });
            }
        });

        return events.toArray(new String[0]);
    }

    /**
     * Get pending events for a workflow
     */
    public Multi<EventPayload> streamPendingEvents(String workflowId) {
        Queue<EventPayload> events = pendingEvents.get(workflowId);
        if (events == null || events.isEmpty()) {
            return Multi.createFrom().empty();
        }

        List<EventPayload> eventList = new ArrayList<>(events);
        return Multi.createFrom().items(eventList.stream());
    }
}
