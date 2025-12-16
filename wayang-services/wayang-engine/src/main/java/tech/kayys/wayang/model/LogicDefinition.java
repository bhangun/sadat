package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.graphql.Ignore;

/**
 * LogicDefinition - Workflow logic layer (nodes + connections)
 */
public class LogicDefinition {
    public List<NodeDefinition> nodes = new ArrayList<>();
    public List<ConnectionDefinition> connections = new ArrayList<>();
    @Ignore
    public Map<String, Object> rules = new HashMap<>();
    @Ignore
    public Map<String, PortDescriptor> ports = new HashMap<>();
}
