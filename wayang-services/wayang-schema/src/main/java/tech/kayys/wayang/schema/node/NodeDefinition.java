package tech.kayys.wayang.schema.node;

import java.util.List;

import tech.kayys.wayang.schema.execution.ErrorHandlingConfig;
import tech.kayys.wayang.schema.execution.ExecutionConfig;
import tech.kayys.wayang.schema.execution.ExecutionContext;
import tech.kayys.wayang.schema.execution.WaitFor;
import tech.kayys.wayang.schema.governance.PolicyConfig;
import tech.kayys.wayang.schema.governance.ProvenanceConfig;
import tech.kayys.wayang.schema.governance.ResourceProfile;
import tech.kayys.wayang.schema.governance.TelemetryConfig;
import tech.kayys.wayang.schema.workflow.Trigger;

public class NodeDefinition {
    private String id;
    private String type;
    private String displayName;
    private String description;
    private String pluginRef;
    private String agentRef;
    private String connectorRef;
    private List<PortDescriptor> inputs;
    private Outputs outputs;
    private List<PropertyDescriptor> properties;
    private ExecutionConfig execution;
    private List<Trigger> triggers;
    private PolicyConfig policy;
    private TelemetryConfig telemetry;
    private ProvenanceConfig provenance;
    private ResourceProfile resourceProfile;
    private NodeUI ui;
    private ExecutionContext executionContext;
    private ControlFlow controlFlow;
    private WaitFor waitFor;
    private ErrorHandlingConfig errorHandling;

    public NodeDefinition() {
    }

    public NodeDefinition(String id, String type, List<PortDescriptor> inputs, Outputs outputs,
            List<PropertyDescriptor> properties, ExecutionConfig execution, List<Trigger> triggers,
            PolicyConfig policy, TelemetryConfig telemetry, ProvenanceConfig provenance,
            ResourceProfile resourceProfile, NodeUI ui, ExecutionContext executionContext,
            ControlFlow controlFlow, WaitFor waitFor, ErrorHandlingConfig errorHandling) {
        this.id = id;
        this.type = type;
        this.inputs = inputs;
        this.outputs = outputs;
        this.properties = properties;
        this.execution = execution;
        this.triggers = triggers;
        this.policy = policy;
        this.telemetry = telemetry;
        this.provenance = provenance;
        this.resourceProfile = resourceProfile;
        this.ui = ui;
        this.executionContext = executionContext;
        this.controlFlow = controlFlow;
        this.waitFor = waitFor;
        this.errorHandling = errorHandling;
    }

    // Getters and setters with validation
    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Node ID cannot be empty");
        }
        this.id = id;
    }

    public void setErrorHandling(ErrorHandlingConfig errorHandling) {
        this.errorHandling = errorHandling;
    }

    public ErrorHandlingConfig getErrorHandling() {
        return errorHandling;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Node type cannot be empty");
        }
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPluginRef() {
        return pluginRef;
    }

    public void setPluginRef(String pluginRef) {
        this.pluginRef = pluginRef;
    }

    public String getAgentRef() {
        return agentRef;
    }

    public void setAgentRef(String agentRef) {
        this.agentRef = agentRef;
    }

    public String getConnectorRef() {
        return connectorRef;
    }

    public void setConnectorRef(String connectorRef) {
        this.connectorRef = connectorRef;
    }

    public List<PortDescriptor> getInputs() {
        return inputs;
    }

    public void setInputs(List<PortDescriptor> inputs) {
        this.inputs = inputs;
    }

    public Outputs getOutputs() {
        return outputs;
    }

    public void setOutputs(Outputs outputs) {
        this.outputs = outputs;
    }

    public List<PropertyDescriptor> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyDescriptor> properties) {
        this.properties = properties;
    }

    public ExecutionConfig getExecution() {
        return execution;
    }

    public void setExecution(ExecutionConfig execution) {
        this.execution = execution;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
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

    public ResourceProfile getResourceProfile() {
        return resourceProfile;
    }

    public void setResourceProfile(ResourceProfile resourceProfile) {
        this.resourceProfile = resourceProfile;
    }

    public NodeUI getUi() {
        return ui;
    }

    public void setUi(NodeUI ui) {
        this.ui = ui;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public ControlFlow getControlFlow() {
        return controlFlow;
    }

    public void setControlFlow(ControlFlow controlFlow) {
        this.controlFlow = controlFlow;
    }

    public WaitFor getWaitFor() {
        return waitFor;
    }

    public void setWaitFor(WaitFor waitFor) {
        this.waitFor = waitFor;
    }
}
