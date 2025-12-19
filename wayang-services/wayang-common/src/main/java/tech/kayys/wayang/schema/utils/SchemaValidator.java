package tech.kayys.wayang.schema.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tech.kayys.wayang.schema.agent.AgentDefinition;
import tech.kayys.wayang.schema.execution.ExecutionConfig;
import tech.kayys.wayang.schema.execution.ValidationResult;
import tech.kayys.wayang.schema.governance.AuthConfig;
import tech.kayys.wayang.schema.governance.Identifier;
import tech.kayys.wayang.schema.governance.ResourceProfile;
import tech.kayys.wayang.schema.llm.ModelSpec;
import tech.kayys.wayang.schema.node.ConnectorDefinition;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.node.OutputChannel;
import tech.kayys.wayang.schema.node.Outputs;
import tech.kayys.wayang.schema.node.PluginDescriptor;
import tech.kayys.wayang.schema.node.PluginImplementation;
import tech.kayys.wayang.schema.node.PortData;
import tech.kayys.wayang.schema.node.PortDescriptor;
import tech.kayys.wayang.schema.orchestration.OrchestrationSpec;
import tech.kayys.wayang.schema.orchestration.OrchestrationTarget;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

/**
 * Validator class for schema constraints
 */
public class SchemaValidator {

    /**
     * Validate an object against schema constraints
     */
    public static ValidationResult validate(Object obj) {
        ValidationResult result = new ValidationResult();

        if (obj == null) {
            result.addError("Object cannot be null");
            return result;
        }

        if (obj instanceof PluginDescriptor) {
            validatePluginDescriptor((PluginDescriptor) obj, result);
        } else if (obj instanceof AgentDefinition) {
            validateAgentDefinition((AgentDefinition) obj, result);
        } else if (obj instanceof WorkflowDefinition) {
            validateWorkflowDefinition((WorkflowDefinition) obj, result);
        } else if (obj instanceof ConnectorDefinition) {
            validateConnectorDefinition((ConnectorDefinition) obj, result);
        } else if (obj instanceof Identifier) {
            validateIdentifier((Identifier) obj, result);
        } else if (obj instanceof PortDescriptor) {
            validatePortDescriptor((PortDescriptor) obj, result);
        }

        return result;
    }

    private static void validatePluginDescriptor(PluginDescriptor plugin, ValidationResult result) {
        if (plugin.getId() == null || plugin.getId().trim().isEmpty()) {
            result.addError("Plugin ID is required");
        } else if (!plugin.getId().matches("^[a-z0-9-]+/[a-z0-9-]+$")) {
            result.addError("Plugin ID must match pattern: [a-z0-9-]+/[a-z0-9-]+");
        }

        if (plugin.getName() == null || plugin.getName().trim().isEmpty()) {
            result.addError("Plugin name is required");
        } else if (plugin.getName().length() < 3 || plugin.getName().length() > 128) {
            result.addError("Plugin name must be between 3 and 128 characters");
        }

        if (plugin.getVersion() == null || plugin.getVersion().trim().isEmpty()) {
            result.addError("Plugin version is required");
        } else if (!isValidSemVer(plugin.getVersion())) {
            result.addError("Version must be valid SemVer format");
        }

        if (plugin.getImplementation() == null) {
            result.addError("Plugin implementation is required");
        } else {
            validatePluginImplementation(plugin.getImplementation(), result);
        }

        if (plugin.getInputs() != null) {
            for (int i = 0; i < plugin.getInputs().size(); i++) {
                PortDescriptor input = plugin.getInputs().get(i);
                ValidationResult inputResult = validate(input);
                if (!inputResult.isValid()) {
                    result.addError("Input " + i + ": " + inputResult.getErrors().get(0));
                }
            }
        }

        if (plugin.getOutputs() != null) {
            validateOutputs(plugin.getOutputs(), result);
        }

        if (plugin.getSandboxLevel() != null) {
            List<String> validLevels = Arrays.asList("trusted", "semi-trusted", "untrusted");
            if (!validLevels.contains(plugin.getSandboxLevel())) {
                result.addError("Invalid sandbox level. Must be one of: " + validLevels);
            }
        }

        if (plugin.getResourceProfile() != null) {
            validateResourceProfile(plugin.getResourceProfile(), result);
        }
    }

    private static void validatePluginImplementation(PluginImplementation impl, ValidationResult result) {
        if (impl.getType() == null) {
            result.addError("Implementation type is required");
        } else {
            List<String> validTypes = Arrays.asList("maven", "wasm", "container", "python",
                    "jar", "node", "binary");
            if (!validTypes.contains(impl.getType())) {
                result.addError("Invalid implementation type. Must be one of: " + validTypes);
            }
        }

        if (impl.getCoordinate() == null || impl.getCoordinate().trim().isEmpty()) {
            result.addError("Implementation coordinate is required");
        }

        if (impl.getDigest() == null || impl.getDigest().trim().isEmpty()) {
            result.addError("Implementation digest is required");
        } else if (!impl.getDigest().matches("^(sha256|sha512|blake3):[a-f0-9]{64,128}$")) {
            result.addError("Digest must match pattern: (sha256|sha512|blake3):[a-f0-9]{64,128}");
        }
    }

    private static void validatePortDescriptor(PortDescriptor port, ValidationResult result) {
        if (port.getName() == null || port.getName().trim().isEmpty()) {
            result.addError("Port name is required");
        } else if (!port.getName().matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            result.addError("Port name must match pattern: ^[a-zA-Z_][a-zA-Z0-9_]*$");
        }

        if (port.getData() == null) {
            result.addError("Port data is required");
        } else {
            validatePortData(port.getData(), result);
        }
    }

    private static void validatePortData(PortData data, ValidationResult result) {
        if (data.getType() == null) {
            result.addError("Port data type is required");
        } else {
            List<String> validTypes = Arrays.asList("json", "string", "markdown", "number",
                    "boolean", "object", "array", "binary",
                    "file_ref", "image", "audio", "video",
                    "embedding", "vector", "llm_completion",
                    "tool_call", "event", "memory_ref", "rag_ref");
            if (!validTypes.contains(data.getType())) {
                result.addError("Invalid port data type. Must be one of: " + validTypes);
            }
        }

        if (data.getFormat() != null) {
            List<String> validFormats = Arrays.asList("text", "html", "yaml", "base64", "uri",
                    "sse", "stream", "jwt", "sql", "graphql",
                    "protobuf");
            if (!validFormats.contains(data.getFormat())) {
                result.addError("Invalid port format. Must be one of: " + validFormats);
            }
        }

        if (data.getMultiplicity() != null) {
            List<String> validMultiplicities = Arrays.asList("single", "list", "map", "stream");
            if (!validMultiplicities.contains(data.getMultiplicity())) {
                result.addError("Invalid multiplicity. Must be one of: " + validMultiplicities);
            }
        }

        if (data.getSource() != null) {
            List<String> validSources = Arrays.asList("input", "context", "rag", "memory",
                    "environment", "secret");
            if (!validSources.contains(data.getSource())) {
                result.addError("Invalid source. Must be one of: " + validSources);
            }
        }
    }

    private static void validateOutputs(Outputs outputs, ValidationResult result) {
        if (outputs.getChannels() != null) {
            for (int i = 0; i < outputs.getChannels().size(); i++) {
                OutputChannel channel = outputs.getChannels().get(i);
                if (channel.getType() != null) {
                    List<String> validTypes = Arrays.asList("success", "error", "conditional",
                            "agent_decision", "stream", "event",
                            "fallback");
                    if (!validTypes.contains(channel.getType())) {
                        result.addError("Output channel " + i + " has invalid type");
                    }
                }
            }
        }
    }

    private static void validateResourceProfile(ResourceProfile profile, ValidationResult result) {
        if (profile.getCpu() != null && !profile.getCpu().matches("^\\d+m?$")) {
            result.addError("CPU must match pattern: ^\\d+m?$");
        }

        if (profile.getMemory() != null && !profile.getMemory().matches("^\\d+[KMG]i?$")) {
            result.addError("Memory must match pattern: ^\\d+[KMG]i?$");
        }

        if (profile.getGpu() < 0) {
            result.addError("GPU count cannot be negative");
        }

        if (profile.getTimeoutMs() < 100) {
            result.addError("Timeout must be at least 100ms");
        }
    }

    private static void validateAgentDefinition(AgentDefinition agent, ValidationResult result) {
        if (agent.getId() == null) {
            result.addError("Agent ID is required");
        } else {
            validateIdentifier(agent.getId(), result);
        }

        if (agent.getName() == null || agent.getName().trim().isEmpty()) {
            result.addError("Agent name is required");
        }

        if (agent.getModelSpec() == null) {
            result.addError("Agent model specification is required");
        } else {
            validateModelSpec(agent.getModelSpec(), result);
        }

        if (agent.getSandboxLevel() != null) {
            List<String> validLevels = Arrays.asList("trusted", "semi-trusted", "untrusted");
            if (!validLevels.contains(agent.getSandboxLevel())) {
                result.addError("Invalid sandbox level. Must be one of: " + validLevels);
            }
        }

        if (agent.getOrchestration() != null) {
            validateOrchestrationSpec(agent.getOrchestration(), result);
        }
    }

    private static void validateModelSpec(ModelSpec spec, ValidationResult result) {
        if (spec.getProvider() == null || spec.getProvider().trim().isEmpty()) {
            result.addError("Model provider is required");
        }

        if (spec.getModel() == null || spec.getModel().trim().isEmpty()) {
            result.addError("Model name is required");
        }

        if (spec.getTemperature() != null && (spec.getTemperature() < 0 || spec.getTemperature() > 2)) {
            result.addError("Temperature must be between 0 and 2");
        }

        if (spec.getTopP() != null && (spec.getTopP() < 0 || spec.getTopP() > 1)) {
            result.addError("TopP must be between 0 and 1");
        }

        if (spec.getMaxTokens() != null && spec.getMaxTokens() < 1) {
            result.addError("Max tokens must be at least 1");
        }
    }

    private static void validateWorkflowDefinition(WorkflowDefinition workflow, ValidationResult result) {
        if (workflow.getId() == null) {
            result.addError("Workflow ID is required");
        } else {
            validateIdentifier(workflow.getId(), result);
        }

        if (workflow.getName() == null || workflow.getName().trim().isEmpty()) {
            result.addError("Workflow name is required");
        }

        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            result.addError("Workflow must have at least one node");
        } else {
            for (int i = 0; i < workflow.getNodes().size(); i++) {
                NodeDefinition node = workflow.getNodes().get(i);
                ValidationResult nodeResult = validate(node);
                if (!nodeResult.isValid()) {
                    result.addError("Node " + i + " (" + node.getId() + "): " +
                            nodeResult.getErrors().get(0));
                }
            }
        }

        if (workflow.getEnvironment() != null) {
            List<String> validEnvs = Arrays.asList("dev", "staging", "prod");
            if (!validEnvs.contains(workflow.getEnvironment())) {
                result.addError("Invalid environment. Must be one of: " + validEnvs);
            }
        }

        if (workflow.getEdges() != null) {
            validateEdges(workflow.getEdges(), workflow.getNodes(), result);
        }
    }

    private static void validateNodeDefinition(NodeDefinition node, ValidationResult result) {
        if (node.getId() == null || node.getId().trim().isEmpty()) {
            result.addError("Node ID is required");
        }

        if (node.getType() == null || node.getType().trim().isEmpty()) {
            result.addError("Node type is required");
        }

        if (node.getInputs() != null) {
            for (int i = 0; i < node.getInputs().size(); i++) {
                PortDescriptor input = node.getInputs().get(i);
                ValidationResult inputResult = validate(input);
                if (!inputResult.isValid()) {
                    result.addError("Input " + i + ": " + inputResult.getErrors().get(0));
                }
            }
        }

        if (node.getOutputs() != null) {
            validateOutputs(node.getOutputs(), result);
        }

        if (node.getExecution() != null) {
            validateExecutionConfig(node.getExecution(), result);
        }
    }

    private static void validateExecutionConfig(ExecutionConfig config, ValidationResult result) {
        if (config.getMode() != null) {
            List<String> validModes = Arrays.asList("sync", "async", "stream");
            if (!validModes.contains(config.getMode())) {
                result.addError("Invalid execution mode. Must be one of: " + validModes);
            }
        }

        if (config.getTimeoutMs() != null && config.getTimeoutMs() < 0) {
            result.addError("Timeout cannot be negative");
        }
    }

    private static void validateEdges(List<EdgeDefinition> edges, List<NodeDefinition> nodes, ValidationResult result) {
        Set<String> nodeIds = new HashSet<>();
        for (NodeDefinition node : nodes) {
            nodeIds.add(node.getId());
        }

        for (EdgeDefinition edge : edges) {
            if (!nodeIds.contains(edge.getFrom())) {
                result.addError("Edge references non-existent source node: " + edge.getFrom());
            }

            if (!nodeIds.contains(edge.getTo())) {
                result.addError("Edge references non-existent target node: " + edge.getTo());
            }
        }
    }

    private static void validateConnectorDefinition(ConnectorDefinition connector, ValidationResult result) {
        if (connector.getId() == null) {
            result.addError("Connector ID is required");
        } else {
            validateIdentifier(connector.getId(), result);
        }

        if (connector.getName() == null || connector.getName().trim().isEmpty()) {
            result.addError("Connector name is required");
        }

        if (connector.getType() == null) {
            result.addError("Connector type is required");
        } else {
            List<String> validTypes = Arrays.asList("http", "graphql", "mq", "database",
                    "custom", "cloud");
            if (!validTypes.contains(connector.getType())) {
                result.addError("Invalid connector type. Must be one of: " + validTypes);
            }
        }

        if (connector.getEndpoint() == null || connector.getEndpoint().trim().isEmpty()) {
            result.addError("Connector endpoint is required");
        }

        if (connector.getAuth() != null) {
            validateAuthConfig(connector.getAuth(), result);
        }
    }

    private static void validateAuthConfig(AuthConfig auth, ValidationResult result) {
        if (auth.getScheme() != null) {
            List<String> validSchemes = Arrays.asList("none", "basic", "bearer", "oauth2",
                    "apiKey", "aws_sigv4");
            if (!validSchemes.contains(auth.getScheme())) {
                result.addError("Invalid auth scheme. Must be one of: " + validSchemes);
            }
        }
    }

    private static void validateIdentifier(Identifier id, ValidationResult result) {
        if (id.getValue() == null || id.getValue().trim().isEmpty()) {
            result.addError("Identifier value is required");
        } else if (!id.getValue().matches("^[a-z0-9_.-]+(/[a-z0-9_.-]+)?$")) {
            result.addError("Identifier must match pattern: ^[a-z0-9_.-]+(/[a-z0-9_.-]+)?$");
        }
    }

    private static void validateOrchestrationSpec(OrchestrationSpec spec, ValidationResult result) {
        if (spec.getStrategy() == null) {
            result.addError("Orchestration strategy is required");
        } else {
            List<String> validStrategies = Arrays.asList("sequential", "parallel", "conditional",
                    "dynamic", "planner_executor", "map_reduce");
            if (!validStrategies.contains(spec.getStrategy())) {
                result.addError("Invalid orchestration strategy. Must be one of: " + validStrategies);
            }
        }

        if (spec.getTargets() == null || spec.getTargets().isEmpty()) {
            result.addError("Orchestration must have at least one target");
        } else {
            for (OrchestrationTarget target : spec.getTargets()) {
                validateOrchestrationTarget(target, result);
            }
        }

        if (spec.getMaxConcurrency() != null && spec.getMaxConcurrency() < 1) {
            result.addError("Max concurrency must be at least 1");
        }

        if (spec.getTimeoutMs() != null && spec.getTimeoutMs() < 0) {
            result.addError("Timeout cannot be negative");
        }

        if (spec.getFailureStrategy() != null) {
            List<String> validStrategies = Arrays.asList("fail_fast", "retry", "skip",
                    "fallback", "escalate");
            if (!validStrategies.contains(spec.getFailureStrategy())) {
                result.addError("Invalid failure strategy. Must be one of: " + validStrategies);
            }
        }
    }

    private static void validateOrchestrationTarget(OrchestrationTarget target, ValidationResult result) {
        if (target.getRefType() == null) {
            result.addError("Target ref type is required");
        } else {
            List<String> validRefTypes = Arrays.asList("agent", "node", "connector",
                    "workflow", "external_agent");
            if (!validRefTypes.contains(target.getRefType())) {
                result.addError("Invalid ref type. Must be one of: " + validRefTypes);
            }
        }

        if (target.getRef() == null || target.getRef().trim().isEmpty()) {
            result.addError("Target ref is required");
        }

        if (target.getTimeoutMs() != null && target.getTimeoutMs() < 0) {
            result.addError("Timeout cannot be negative");
        }
    }

    private static boolean isValidSemVer(String version) {
        return version.matches("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?$");
    }

}
