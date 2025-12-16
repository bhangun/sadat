package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.graphql.Ignore;

/**
 * NodeDefinition - Individual node in workflow
 */
public class NodeDefinition {
    public String id; // Unique within workflow
    public String type; // Node type (plugin ID)
    public String name;
    public String description;
    @Ignore
    public Map<String, Object> properties = new HashMap<>();
    public List<PortDescriptor> inputs = new ArrayList<>();
    public List<OutputChannel> outputs = new ArrayList<>();
    @Ignore
    public Map<String, Object> errorHandling;
    public ResourceProfile resourceProfile;
    @Ignore
    public Map<String, Object> metadata = new HashMap<>();
}
