package tech.kayys.agent.schema;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
package tech.kayys.platform.schema;

import java.net.URI;
import java.util.*;

public record AgentDefinition(
    String id,
    String name,
    Optional<String> description,
    List<AgentCapability> capabilities,
    Optional<MemoryProfile> memoryProfile,
    SecurityPolicy securityPolicy,
    Optional<Endpoint> endpoint,
    URI schemaRef,
    Optional<String> checksum
) {
    public static final URI DEFAULT_SCHEMA_REF = 
        URI.create("https://schemas.kayys.tech/agent/v1");

    public AgentDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        capabilities = List.copyOf(capabilities);
        if (securityPolicy == null) {
            securityPolicy = SecurityPolicy.defaultPolicy();
        }
        if (schemaRef == null) {
            schemaRef = DEFAULT_SCHEMA_REF;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private List<AgentCapability> capabilities = new ArrayList<>();
        private MemoryProfile memoryProfile;
        private SecurityPolicy securityPolicy;
        private Endpoint endpoint;
        private URI schemaRef = DEFAULT_SCHEMA_REF;
        private String checksum;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String desc) { this.description = desc; return this; }
        public Builder capabilities(List<AgentCapability> caps) { this.capabilities = new ArrayList<>(caps); return this; }
        public Builder addCapability(AgentCapability cap) { this.capabilities.add(cap); return this; }
        public Builder memoryProfile(MemoryProfile profile) { this.memoryProfile = profile; return this; }
        public Builder securityPolicy(SecurityPolicy policy) { this.securityPolicy = policy; return this; }
        public Builder endpoint(Endpoint endpoint) { this.endpoint = endpoint; return this; }
        public Builder schemaRef(URI ref) { this.schemaRef = ref; return this; }
        public Builder checksum(String checksum) { this.checksum = checksum; return this; }

        public AgentDefinition build() {
            return new AgentDefinition(
                id,
                name,
                Optional.ofNullable(description),
                capabilities,
                Optional.ofNullable(memoryProfile),
                securityPolicy,
                Optional.ofNullable(endpoint),
                schemaRef,
                Optional.ofNullable(checksum)
            );
        }
    }
}
