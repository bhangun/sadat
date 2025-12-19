package tech.kayys.wayang.schema.workflow;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.schema.execution.ValidationResult;
import tech.kayys.wayang.schema.governance.DataContract;
import tech.kayys.wayang.schema.governance.Identifier;
import tech.kayys.wayang.schema.governance.PolicyConfig;
import tech.kayys.wayang.schema.governance.ProvenanceConfig;
import tech.kayys.wayang.schema.governance.SimulationConfig;
import tech.kayys.wayang.schema.governance.TelemetryConfig;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.utils.SchemaValidator;

public class WorkflowDefinition {
    private Identifier id;
    private String name;
    private String description;
    private String version;
    private String createdAt;
    private String createdBy;
    private String tenantId;
    private String environment = "dev";
    private List<NodeDefinition> nodes;
    private List<EdgeDefinition> edges;
    private List<Trigger> triggers;
    private WorkflowState state;
    private List<DataContract> dataContracts;
    private PolicyConfig policy;
    private TelemetryConfig telemetry;
    private ProvenanceConfig provenance;
    private SimulationConfig simulation;
    private List<String> comments;
    private Map<String, Object> metadata;

    public WorkflowDefinition() {
    }

    public WorkflowDefinition(String id, String name, List<NodeDefinition> nodes) {
        this.id = new Identifier(id);
        this.name = name;
        this.nodes = nodes;
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
            throw new IllegalArgumentException("Workflow name cannot be empty");
        }
        this.name = name;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version != null && !version.matches("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?$")) {
            throw new IllegalArgumentException("Invalid SemVer format");
        }
        this.version = version;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        List<String> validEnvs = Arrays.asList("dev", "staging", "prod");
        if (!validEnvs.contains(environment)) {
            throw new IllegalArgumentException("Invalid environment: " + environment);
        }
        this.environment = environment;
    }

    public List<NodeDefinition> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDefinition> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one node");
        }
        this.nodes = nodes;
    }

    public List<EdgeDefinition> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeDefinition> edges) {
        this.edges = edges;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public WorkflowState getState() {
        return state;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public List<DataContract> getDataContracts() {
        return dataContracts;
    }

    public void setDataContracts(List<DataContract> dataContracts) {
        this.dataContracts = dataContracts;
    }

    public PolicyConfig getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyConfig policy) {
        this.policy = policy;
    }

    public TelemetryConfig getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryConfig telemetry) {
        this.telemetry = telemetry;
    }

    public ProvenanceConfig getProvenance() {
        return provenance;
    }

    public void setProvenance(ProvenanceConfig provenance) {
        this.provenance = provenance;
    }

    public SimulationConfig getSimulation() {
        return simulation;
    }

    public void setSimulation(SimulationConfig simulation) {
        this.simulation = simulation;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    private void validateIdentifier(Identifier id) {
        if (id == null) {
            throw new IllegalArgumentException("Workflow ID cannot be null");
        }
        ValidationResult result = SchemaValidator.validate(id);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Invalid identifier: " + result.getErrors().get(0));
        }
    }
}
