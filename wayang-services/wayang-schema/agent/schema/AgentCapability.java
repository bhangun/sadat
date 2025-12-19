package tech.kayys.agent.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import tech.kayys.agent.AgentStatus;

import java.net.URI;
import java.util.*;

public record AgentCapability(
    String capabilityId,
    String agentId,
    String name,
    Optional<String> description,
    Set<String> requiredPermissions,     // e.g., "tool:calculator:execute"
    boolean isAvailable,
    List<String> supportedMethods,       // e.g., ["GET", "POST"]
    List<String> domains,                // e.g., ["finance", "research"]
    int priority,
    Optional<URI> endpoint,              // ← URI ensures valid format
    AgentStatus status,                  // ← your enum!
    List<ExtensionPoint> extensions,      // ← replaces untyped metadata
    List<ActionParameter> parameters,
    List<String> produces,
    List<String> requires,
    CapabilityProfile profile
) {
    // Private constructor for validation & normalization
    public AgentCapability {
        Objects.requireNonNull(capabilityId, "capabilityId must not be null");
        Objects.requireNonNull(agentId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(requiredPermissions);
        Objects.requireNonNull(supportedMethods);
        Objects.requireNonNull(domains);
        Objects.requireNonNull(status);

        // Defensive copies
        requiredPermissions = Set.copyOf(requiredPermissions);
        supportedMethods = List.copyOf(supportedMethods);
        domains = List.copyOf(domains);
        extensions = List.copyOf(extensions);

        // Normalize parameters
        parameters = List.copyOf(parameters);

          produces = List.copyOf(produces);
        requires = List.copyOf(requires);

        // Validate URI format if present
        if (endpoint != null && endpoint.toString().isBlank()) {
            endpoint = null;
        }
    }

    // Builder for ergonomics
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
                private String id;
        private String capabilityId;
        private String agentId;
        private String name;
        private String description;
        private Set<String> requiredPermissions = new LinkedHashSet<>();
              private List<ActionParameter> parameters = new ArrayList<>();
        private List<String> produces = new ArrayList<>();
        private List<String> requires = new ArrayList<>();
        private CapabilityProfile profile;
        private boolean isAvailable = true;
        private List<String> supportedMethods = new ArrayList<>();
        private List<String> domains = new ArrayList<>();
        private int priority = 0;
        private URI endpoint;
        private AgentStatus status = AgentStatus.ACTIVE;
        private List<ExtensionPoint> extensions = new ArrayList<>();

        public Builder capabilityId(String id) { this.capabilityId = id; return this; }
        public Builder agentId(String id) { this.agentId = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String desc) { this.description = desc; return this; }
        public Builder requiredPermissions(Set<String> perms) { this.requiredPermissions = new LinkedHashSet<>(perms); return this; }
        public Builder addPermission(String perm) { this.requiredPermissions.add(perm); return this; }
  
        public Builder isAvailable(boolean available) { this.isAvailable = available; return this; }
        public Builder supportedMethods(List<String> methods) { this.supportedMethods = new ArrayList<>(methods); return this; }
        public Builder addMethod(String method) { this.supportedMethods.add(method); return this; }
        public Builder domains(List<String> domains) { this.domains = new ArrayList<>(domains); return this; }
        public Builder addDomain(String domain) { this.domains.add(domain); return this; }
        public Builder priority(int p) { this.priority = p; return this; }
        public Builder endpoint(URI uri) { this.endpoint = uri; return this; }
        public Builder status(AgentStatus status) { this.status = status; return this; }
        public Builder extensions(List<ExtensionPoint> exts) { this.extensions = new ArrayList<>(exts); return this; }
        public Builder addExtension(ExtensionPoint ext) { this.extensions.add(ext); return this; }
        public Builder parameters(List<ActionParameter> params) { this.parameters = new ArrayList<>(params); return this; }
        public Builder addParameter(ActionParameter param) { this.parameters.add(param); return this; }
        public Builder produces(List<String> types) { this.produces = new ArrayList<>(types); return this; }
        public Builder addProduces(String type) { this.produces.add(type); return this; }
        public Builder requires(List<String> reqs) { this.requires = new ArrayList<>(reqs); return this; }
        public Builder addRequires(String req) { this.requires.add(req); return this; }
        public Builder profile(CapabilityProfile profile) { this.profile = profile; return this; }

        public AgentCapability build() {
            return new AgentCapability(id, name, domains, parameters, produces, requires, profile);
        }
    }
}