package tech.kayys.node;

import com.fasterxml.jackson.databind.jsonschema.JsonSchema;

public class OutputPort {
    String name;
    DataType type;
    JsonSchema schema;
}