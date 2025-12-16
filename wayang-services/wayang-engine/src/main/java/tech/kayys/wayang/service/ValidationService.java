package tech.kayys.wayang.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.kayys.wayang.client.GuardrailsClient;
import tech.kayys.wayang.client.SchemaRegistryClient;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.model.ConnectionDefinition;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.NodeDefinition;
import tech.kayys.wayang.model.ValidationResult;

/**
 * ValidationService - Workflow validation logic
 */
@ApplicationScoped
public class ValidationService {

    private static final Logger LOG = Logger.getLogger(ValidationService.class);

    @Inject
    @RestClient
    SchemaRegistryClient schemaRegistryClient;

    @Inject
    @RestClient
    GuardrailsClient guardrailsClient;

    /**
     * Validate complete workflow
     */
    public Uni<ValidationResult> validateWorkflow(Workflow workflow) {
        LOG.infof("Validating workflow: %s", workflow.id);

        ValidationResult result = new ValidationResult();
        result.validatedAt = Instant.now();
        result.validatorVersion = "1.0.0";

        return validateLogic(workflow.logic)
                .map(logicResult -> {
                    result.errors.addAll(logicResult.errors);
                    result.warnings.addAll(logicResult.warnings);
                    result.valid = logicResult.valid;
                    return result;
                });
    }

    /**
     * Validate logic definition
     */
    public Uni<ValidationResult> validateLogic(LogicDefinition logic) {
        ValidationResult result = new ValidationResult();
        result.validatedAt = Instant.now();
        result.validatorVersion = "1.0.0";

        // Basic structure validation
        if (logic.nodes == null || logic.nodes.isEmpty()) {
            result.errors.add(createError("EMPTY_WORKFLOW",
                    "Workflow must contain at least one node", null));
            result.valid = false;
            return Uni.createFrom().item(result);
        }

        // Validate each node
        List<Uni<Void>> nodeValidations = logic.nodes.stream()
                .map(node -> validateNode(node, result))
                .toList();

        return Uni.join().all(nodeValidations).andFailFast()
                .replaceWith(result)
                .flatMap(r -> validateConnections(logic, r))
                .flatMap(r -> validateTopology(logic, r))
                .onFailure().recoverWithItem(error -> {
                    LOG.errorf(error, "Validation failed for logic");
                    result.errors.add(createError("VALIDATION_FAILED",
                            "Internal validation error: " + error.getMessage(), null));
                    result.valid = false;
                    return result;
                });
    }

    /**
     * Validate individual node
     */
    private Uni<Void> validateNode(NodeDefinition node, ValidationResult result) {
        // Validate node structure
        if (node.id == null || node.id.isBlank()) {
            result.errors.add(createError("MISSING_NODE_ID",
                    "Node must have an ID", null));
            result.valid = false;
            return Uni.createFrom().voidItem();
        }

        if (node.type == null || node.type.isBlank()) {
            result.errors.add(createError("MISSING_NODE_TYPE",
                    "Node must have a type", node.id));
            result.valid = false;
            return Uni.createFrom().voidItem();
        }

        // Validate node type exists in schema registry
        return schemaRegistryClient.getNodeSchema(node.type)
                .onItem().ifNull().continueWith(() -> {
                    result.errors.add(createError("UNKNOWN_NODE_TYPE",
                            "Unknown node type: " + node.type, node.id));
                    result.valid = false;
                    return null;
                })
                .replaceWithVoid();
    }

    /**
     * Validate connections
     */
    private Uni<ValidationResult> validateConnections(LogicDefinition logic, ValidationResult result) {
        Set<String> nodeIds = logic.nodes.stream()
                .map(n -> n.id)
                .collect(java.util.stream.Collectors.toSet());

        for (ConnectionDefinition conn : logic.connections) {
            // Validate connection structure
            if (conn.from == null || conn.to == null) {
                result.errors.add(createError("INVALID_CONNECTION",
                        "Connection must have from and to nodes", conn.id));
                result.valid = false;
                continue;
            }

            // Validate nodes exist
            if (!nodeIds.contains(conn.from)) {
                result.errors.add(createError("INVALID_SOURCE_NODE",
                        "Source node not found: " + conn.from, conn.id));
                result.valid = false;
            }

            if (!nodeIds.contains(conn.to)) {
                result.errors.add(createError("INVALID_TARGET_NODE",
                        "Target node not found: " + conn.to, conn.id));
                result.valid = false;
            }

            // Validate port compatibility (simplified)
            if (conn.fromPort == null || conn.toPort == null) {
                result.warnings.add(createWarning(
                        "Connection missing port names", conn.id,
                        "Specify explicit port names for better clarity"));
            }
        }

        return Uni.createFrom().item(result);
    }

    /**
     * Validate workflow topology
     */
    private Uni<ValidationResult> validateTopology(LogicDefinition logic, ValidationResult result) {
        // Check for cycles (simplified - should use proper graph algorithm)
        Set<String> visited = new HashSet<>();
        Set<String> inProgress = new HashSet<>();

        for (NodeDefinition node : logic.nodes) {
            if (hasCycle(node.id, logic, visited, inProgress)) {
                result.warnings.add(createWarning(
                        "Workflow contains cycles", node.id,
                        "Cycles may cause infinite loops unless properly handled"));
                break;
            }
        }

        // Check for disconnected nodes
        Set<String> connectedNodes = new HashSet<>();
        for (ConnectionDefinition conn : logic.connections) {
            connectedNodes.add(conn.from);
            connectedNodes.add(conn.to);
        }

        for (NodeDefinition node : logic.nodes) {
            if (!connectedNodes.contains(node.id) && logic.nodes.size() > 1) {
                result.warnings.add(createWarning(
                        "Disconnected node", node.id,
                        "Node is not connected to the workflow"));
            }
        }

        return Uni.createFrom().item(result);
    }

    private boolean hasCycle(String nodeId, LogicDefinition logic,
            Set<String> visited, Set<String> inProgress) {
        if (inProgress.contains(nodeId)) {
            return true;
        }

        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        inProgress.add(nodeId);

        // Find outgoing connections
        for (ConnectionDefinition conn : logic.connections) {
            if (conn.from.equals(nodeId)) {
                if (hasCycle(conn.to, logic, visited, inProgress)) {
                    return true;
                }
            }
        }

        inProgress.remove(nodeId);
        return false;
    }

    private ValidationResult.ValidationError createError(String code, String message, String nodeId) {
        ValidationResult.ValidationError error = new ValidationResult.ValidationError();
        error.code = code;
        error.message = message;
        error.nodeId = nodeId;
        error.severity = ValidationResult.ValidationError.ErrorSeverity.ERROR;
        return error;
    }

    private ValidationResult.ValidationWarning createWarning(String message, String nodeId, String suggestion) {
        ValidationResult.ValidationWarning warning = new ValidationResult.ValidationWarning();
        warning.message = message;
        warning.nodeId = nodeId;
        warning.suggestion = suggestion;
        return warning;
    }
}
