package tech.kayys.wayang.plugin.execution;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.ControlPlaneExecutorRegistry;
import tech.kayys.wayang.plugin.ControlPlaneNodeRegistry;
import tech.kayys.wayang.plugin.SchemaValidator;
import tech.kayys.wayang.plugin.TraceContext;
import tech.kayys.wayang.plugin.ValidationResult;
import tech.kayys.wayang.plugin.executor.ExecutorDescriptor;
import tech.kayys.wayang.plugin.executor.ExecutorRegistration;
import tech.kayys.wayang.plugin.node.NodeDefinition;
import tech.kayys.wayang.plugin.node.NodeDescriptor;

/**
 * Builder for creating execution contracts
 */
@ApplicationScoped
public class ExecutionContractBuilder {

        private static final Logger LOG = Logger.getLogger(ExecutionContractBuilder.class);

        @Inject
        ControlPlaneNodeRegistry nodeRegistry;

        @Inject
        ControlPlaneExecutorRegistry executorRegistry;

        @Inject
        SchemaValidator schemaValidator;

        /**
         * Build execution contract from node execution request
         */
        public Uni<ExecutionContract> build(
                        String workflowRunId,
                        String nodeType,
                        String nodeInstanceId,
                        Map<String, Object> inputs,
                        Map<String, Object> config,
                        ExecutionContext context) {

                return Uni.createFrom().item(() -> {
                        // Get node definition
                        NodeDefinition node = nodeRegistry.get(nodeType);
                        if (node == null) {
                                throw new IllegalArgumentException("Node type not found: " + nodeType);
                        }

                        // Validate inputs
                        ValidationResult inputValidation = schemaValidator.validate(
                                        node.inputSchema, inputs);
                        if (!inputValidation.isValid()) {
                                throw new IllegalArgumentException(
                                                "Input validation failed: " + inputValidation.getMessage());
                        }

                        // Validate config
                        ValidationResult configValidation = schemaValidator.validate(
                                        node.configSchema, config);
                        if (!configValidation.isValid()) {
                                throw new IllegalArgumentException(
                                                "Config validation failed: " + configValidation.getMessage());
                        }

                        // Resolve executor
                        ExecutorRegistration executor = executorRegistry.resolveForNode(nodeType);
                        if (executor == null) {
                                throw new IllegalStateException(
                                                "No healthy executor found for node: " + nodeType);
                        }

                        // Build contract
                        ExecutionContract contract = new ExecutionContract();
                        contract.executionId = UUID.randomUUID().toString();
                        contract.workflowRunId = workflowRunId;

                        // Node descriptor
                        contract.node = new NodeDescriptor();
                        contract.node.type = nodeType;
                        contract.node.version = node.version;
                        contract.node.instanceId = nodeInstanceId;

                        // Executor descriptor
                        contract.executor = new ExecutorDescriptor();
                        contract.executor.executorId = executor.executorId;
                        contract.executor.capabilities = new HashSet<>(executor.capabilities);
                        contract.executor.endpoint = executor.endpoint;
                        contract.executor.protocol = executor.protocol;

                        // Execution mode
                        contract.mode = node.executorBinding.mode;

                        // Data
                        contract.inputs = new HashMap<>(inputs);
                        contract.config = new HashMap<>(config);
                        contract.context = context;

                        // Tracing
                        contract.trace = new TraceContext();
                        contract.trace.traceId = UUID.randomUUID().toString();
                        contract.trace.spanId = UUID.randomUUID().toString();

                        // Timing
                        contract.createdAt = Instant.now();
                        contract.expiresAt = Instant.now().plusSeconds(
                                        executor.metadata.timeoutMs / 1000);

                        LOG.infof("Built execution contract: %s for node %s using executor %s",
                                        contract.executionId, nodeType, executor.executorId);

                        return contract;
                });
        }
}
