package tech.kayys.wayang.project.dto;

import java.util.List;
import java.util.Map;

/**
 * Agent Tool - Tools available to agent
 */
public class AgentTool {
    public String toolId;
    public String name;
    public String description;
    public ToolType type;
    public Map<String, Object> config;
    public List<String> permissions;
}
