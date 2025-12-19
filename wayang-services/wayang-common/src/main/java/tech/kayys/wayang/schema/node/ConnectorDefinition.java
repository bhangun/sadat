package tech.kayys.wayang.schema.node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.schema.execution.ValidationResult;
import tech.kayys.wayang.schema.governance.AuthConfig;
import tech.kayys.wayang.schema.governance.Identifier;
import tech.kayys.wayang.schema.governance.PolicyConfig;
import tech.kayys.wayang.schema.governance.ResourceProfile;
import tech.kayys.wayang.schema.utils.SchemaValidator;

public class ConnectorDefinition {
    private Identifier id;
    private String name;
    private String type;
    private String description;
    private String endpoint;
    private AuthConfig auth;
    private List<PortDescriptor> inputs;
    private Outputs outputs;
    private Map<String, Object> rateLimit;
    private ResourceProfile resourceProfile;
    private PolicyConfig policy;
    private Map<String, Object> ui;

    public ConnectorDefinition() {
    }

    public ConnectorDefinition(String id, String name, String type, String endpoint) {
        this.id = new Identifier(id);
        this.name = name;
        this.type = type;
        this.endpoint = endpoint;
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
            throw new IllegalArgumentException("Connector name cannot be empty");
        }
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        List<String> validTypes = Arrays.asList("http", "graphql", "mq", "database",
                "custom", "cloud");
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("Invalid connector type: " + type);
        }
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Connector endpoint cannot be empty");
        }
        this.endpoint = endpoint;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
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

    public Map<String, Object> getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Map<String, Object> rateLimit) {
        this.rateLimit = rateLimit;
    }

    public ResourceProfile getResourceProfile() {
        return resourceProfile;
    }

    public void setResourceProfile(ResourceProfile resourceProfile) {
        this.resourceProfile = resourceProfile;
    }

    public PolicyConfig getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyConfig policy) {
        this.policy = policy;
    }

    public Map<String, Object> getUi() {
        return ui;
    }

    public void setUi(Map<String, Object> ui) {
        this.ui = ui;
    }

    private void validateIdentifier(Identifier id) {
        if (id == null) {
            throw new IllegalArgumentException("Connector ID cannot be null");
        }
        ValidationResult result = SchemaValidator.validate(id);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Invalid identifier: " + result.getErrors().get(0));
        }
    }
}
