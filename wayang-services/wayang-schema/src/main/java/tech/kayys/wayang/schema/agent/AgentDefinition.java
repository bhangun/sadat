package tech.kayys.wayang.schema.agent;

import java.util.Arrays;
import java.util.List;

import tech.kayys.wayang.schema.acp.CommercePolicy;
import tech.kayys.wayang.schema.execution.ValidationResult;
import tech.kayys.wayang.schema.governance.Identifier;
import tech.kayys.wayang.schema.governance.PolicyConfig;
import tech.kayys.wayang.schema.governance.ProvenanceConfig;
import tech.kayys.wayang.schema.llm.ModelSpec;
import tech.kayys.wayang.schema.llm.ToolDefinition;
import tech.kayys.wayang.schema.orchestration.OrchestrationSpec;
import tech.kayys.wayang.schema.utils.SchemaValidator;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

public class AgentDefinition {
    private Identifier id;
    private String name;
    private String description;
    private List<String> roles = Arrays.asList("worker");
    private ModelSpec modelSpec;
    private List<ToolDefinition> tools;
    private MemoryConfig memory;
    private PolicyConfig policy;
    private ProvenanceConfig provenance;
    private String sandboxLevel = "untrusted";
    private OrchestrationSpec orchestration;
    private CommercePolicy commerce;
    private List<WorkflowDefinition> workflows;

    public AgentDefinition() {
    }

    public AgentDefinition(String id, String name, ModelSpec modelSpec) {
        this.id = new Identifier(id);
        this.name = name;
        this.modelSpec = modelSpec;
    }

    // Getters and setters with validation
    public Identifier getId() {
        return id;
    }

    public void setId(Identifier id) {
        validateIdentifier(id);
        this.id = id;
    }

    public void setId(String id) {
        this.id = new Identifier(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name cannot be empty");
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? roles : Arrays.asList("worker");
    }

    public ModelSpec getModelSpec() {
        return modelSpec;
    }

    public void setModelSpec(ModelSpec modelSpec) {
        if (modelSpec == null) {
            throw new IllegalArgumentException("Model specification cannot be null");
        }
        this.modelSpec = modelSpec;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }

    public MemoryConfig getMemory() {
        return memory;
    }

    public void setMemory(MemoryConfig memory) {
        this.memory = memory;
    }

    public PolicyConfig getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyConfig policy) {
        this.policy = policy;
    }

    public ProvenanceConfig getProvenance() {
        return provenance;
    }

    public void setProvenance(ProvenanceConfig provenance) {
        this.provenance = provenance;
    }

    public String getSandboxLevel() {
        return sandboxLevel;
    }

    public void setSandboxLevel(String sandboxLevel) {
        List<String> validLevels = Arrays.asList("trusted", "semi-trusted", "untrusted");
        if (!validLevels.contains(sandboxLevel)) {
            throw new IllegalArgumentException("Invalid sandbox level: " + sandboxLevel);
        }
        this.sandboxLevel = sandboxLevel;
    }

    public OrchestrationSpec getOrchestration() {
        return orchestration;
    }

    public void setOrchestration(OrchestrationSpec orchestration) {
        this.orchestration = orchestration;
    }

    public CommercePolicy getCommerce() {
        return commerce;
    }

    public void setCommerce(CommercePolicy commerce) {
        this.commerce = commerce;
    }

    private void validateIdentifier(Identifier id) {
        if (id == null) {
            throw new IllegalArgumentException("Agent ID cannot be null");
        }
        ValidationResult result = SchemaValidator.validate(id);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Invalid identifier: " + result.getErrors().get(0));
        }
    }

    public List<WorkflowDefinition> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<WorkflowDefinition> workflows) {
        this.workflows = workflows;
    }
}
