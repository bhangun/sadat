package tech.kayys.agent.schema;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Optional;

public record ActionParameter(
    String name,
    String type,
    boolean required,
    Optional<String> description,
    JsonNode schema
) {
    public ActionParameter {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        description = Optional.ofNullable(description.orElse(null));
        schema = (schema == null) ? JsonNodeFactory.instance.objectNode() : schema;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String type;
        private boolean required;
        private String description;
        private JsonNode schema;

        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder required(boolean required) { this.required = required; return this; }
        public Builder description(String desc) { this.description = desc; return this; }
        public Builder schema(JsonNode schema) { this.schema = schema; return this; }

        public ActionParameter build() {
            return new ActionParameter(name, type, required, Optional.ofNullable(description), schema);
        }
    }
}