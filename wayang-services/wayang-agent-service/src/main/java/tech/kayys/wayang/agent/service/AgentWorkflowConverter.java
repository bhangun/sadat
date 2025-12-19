package tech.kayys.wayang.agent.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentWorkflowRequest;
import tech.kayys.wayang.agent.dto.EdgeSpec;
import tech.kayys.wayang.agent.dto.LLMConfig;
import tech.kayys.wayang.agent.dto.NodeSpec;
import tech.kayys.wayang.agent.dto.RAGConfig;
import tech.kayys.wayang.agent.dto.MemoryConfig;
import tech.kayys.wayang.agent.dto.WorkflowDefinition;

/**
 * Converts agent specifications to workflow definitions
 */
@ApplicationScoped
class AgentWorkflowConverter {

    public WorkflowDefinition toWorkflowDefinition(AgentWorkflowRequest request) {
        // For now, returning a mock workflow definition
        // In a real implementation, this would create a proper workflow from the agent request
        return new WorkflowDefinition(
                UUID.randomUUID().toString(),
                request.name(),
                request.description(),
                request.tenantId(),
                "ACTIVE",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                Map.of("nodes", request.nodes(), "edges", request.edges())
        );
    }

    private List<tech.kayys.wayang.agent.dto.NodeSpec> convertNodesToDefinitions(
            List<NodeSpec> specs,
            LLMConfig llmConfig,
            RAGConfig ragConfig,
            MemoryConfig memoryConfig) {
        return specs.stream()
                .map(spec -> new NodeSpec(
                        spec.type(),
                        spec.name(),
                        new HashMap<>(spec.config())
                ))
                .collect(Collectors.toList());
    }

    private List<tech.kayys.wayang.agent.dto.EdgeSpec> convertEdgesToDefinitions(List<EdgeSpec> specs) {
        return specs; // Already in the right format
    }
}