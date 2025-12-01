package tech.kayys.agent.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AgentCapability {
    public String capabilityId;
    public String agentId;
    public String name;
    public String description;
    public Set<String> requiredPermissions;
    public Map<String, Object> parameters;
    public boolean isAvailable;
    public String[] supportedMethods;
    public String[] domains;
    public int priority;
    public String endpoint;
    public String status;
    public Map<String, Object> metadata;

    public static AgentCapability create(String id, String name, String agentId) {
        AgentCapability cap = new AgentCapability();
        cap.capabilityId = id;
        cap.name = name;
        cap.agentId = agentId;
        cap.requiredPermissions = new HashSet<>();
        cap.parameters = new HashMap<>();
        cap.isAvailable = true;
        return cap;
    }
}