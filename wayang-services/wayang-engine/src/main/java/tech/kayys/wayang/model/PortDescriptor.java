package tech.kayys.wayang.model;

import org.eclipse.microprofile.graphql.Ignore;

/**
 * PortDescriptor - Node input/output port definition
 */
public class PortDescriptor {
    public String name;
    public String displayName;
    public String description;
    public DataType data;

    public static class DataType {
        public String type; // json, string, number, etc.
        public String format; // text, html, base64, etc.
        @Ignore
        public Object schema; // JSON Schema
        public Multiplicity multiplicity = Multiplicity.SINGLE;
        public boolean required = true;
        @Ignore
        public Object defaultValue;
        public boolean sensitive = false;

        public enum Multiplicity {
            SINGLE, LIST, MAP, STREAM
        }
    }
}
