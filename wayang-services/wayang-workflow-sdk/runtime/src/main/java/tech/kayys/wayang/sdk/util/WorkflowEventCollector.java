package tech.kayys.wayang.sdk.util;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.sdk.dto.WorkflowExecutionEvent;
import tech.kayys.wayang.sdk.WorkflowRunClient;

/**
 * Utility for collecting and aggregating workflow events
 */
@ApplicationScoped
public class WorkflowEventCollector {

    private final Map<String, MultiEmitter<? super WorkflowExecutionEvent>> emitters = 
        new ConcurrentHashMap<>();

    /**
     * Create event stream for a workflow run
     */
    public Multi<WorkflowExecutionEvent> createEventStream(String runId) {
        return Multi.createFrom().emitter(emitter -> {
            emitters.put(runId, emitter);
            emitter.onTermination(() -> emitters.remove(runId));
        });
    }

    /**
     * Emit event to subscribers
     */
    public void emitEvent(String runId, WorkflowExecutionEvent event) {
        MultiEmitter<? super WorkflowExecutionEvent> emitter = emitters.get(runId);
        if (emitter != null) {
            emitter.emit(event);
        }
    }

    /**
     * Complete event stream
     */
    public void completeStream(String runId) {
        MultiEmitter<? super WorkflowExecutionEvent> emitter = emitters.get(runId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(runId);
        }
    }

    /**
     * Fail event stream
     */
    public void failStream(String runId, Throwable error) {
        MultiEmitter<? super WorkflowExecutionEvent> emitter = emitters.get(runId);
        if (emitter != null) {
            emitter.fail(error);
            emitters.remove(runId);
        }
    }
}
