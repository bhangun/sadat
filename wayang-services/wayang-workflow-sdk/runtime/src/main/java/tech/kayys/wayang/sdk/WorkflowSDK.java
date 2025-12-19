package tech.kayys.wayang.sdk;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




import tech.kayys.wayang.sdk.dto.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Fluent API for easier SDK usage
 * 
 * Example:
 * <pre>
 * WorkflowRun run = workflowSDK
 *     .workflow("order-processing")
 *     .forTenant("acme-corp")
 *     .triggeredBy("user:john")
 *     .withInput("orderId", "12345")
 *     .withPriority(HIGH)
 *     .execute()
 *     .await().indefinitely();
 * </pre>
 */
@ApplicationScoped
public class WorkflowSDK {

    @Inject
    @RestClient
    WorkflowRunClient workflowClient;

    public WorkflowBuilder workflow(String workflowId) {
        return new WorkflowBuilder(workflowClient, workflowId);
    }

    public static class WorkflowBuilder {
        private final WorkflowRunClient client;
        private final String workflowId;
        private String workflowVersion = "1.0.0";
        private String tenantId;
        private String triggeredBy;
        private String correlationId;
        private final Map<String, Object> inputs = new HashMap<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private ExecutionOptions options = ExecutionOptions.defaults();

        WorkflowBuilder(WorkflowRunClient client, String workflowId) {
            this.client = client;
            this.workflowId = workflowId;
        }

        public WorkflowBuilder version(String version) {
            this.workflowVersion = version;
            return this;
        }

        public WorkflowBuilder forTenant(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public WorkflowBuilder triggeredBy(String triggeredBy) {
            this.triggeredBy = triggeredBy;
            return this;
        }

        public WorkflowBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public WorkflowBuilder withInput(String key, Object value) {
            this.inputs.put(key, value);
            return this;
        }

        public WorkflowBuilder withInputs(Map<String, Object> inputs) {
            this.inputs.putAll(inputs);
            return this;
        }

        public WorkflowBuilder withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public WorkflowBuilder withPriority(ExecutionOptions.Priority priority) {
            this.options = options.withPriority(priority);
            return this;
        }

        public WorkflowBuilder withTimeout(long timeoutMs) {
            this.options = options.withTimeout(timeoutMs);
            return this;
        }

        public WorkflowBuilder asDryRun() {
            this.options = options.asDryRun();
            return this;
        }

        public Uni<WorkflowRunResponse> execute() {
            TriggerWorkflowRequest request = new TriggerWorkflowRequest(
                workflowId, workflowVersion, inputs, correlationId
            );
            return client.triggerWorkflow(request);
        }

        public Uni<WorkflowRunResponse> executeAndWait() {
            TriggerWorkflowRequest request = new TriggerWorkflowRequest(
                workflowId, workflowVersion, inputs, correlationId
            );
            return client.triggerWorkflowSync(request, options.timeoutMs());
        }
    }
}
