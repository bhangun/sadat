package tech.kayys.wayang.model;

import org.eclipse.microprofile.graphql.Ignore;

/**
 * OutputChannel - Node output with routing
 */
public class OutputChannel {
    public String name;
    public String displayName;
    public ChannelType type;
    public String condition; // CEL expression
    @Ignore
    public Object schema;

    public enum ChannelType {
        SUCCESS, ERROR, CONDITIONAL, AGENT_DECISION, STREAM, EVENT
    }
}
