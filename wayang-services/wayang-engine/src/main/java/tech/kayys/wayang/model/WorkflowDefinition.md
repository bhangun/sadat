package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a workflow definition containing the DAG structure
 */
public class WorkflowDefinition {
    private String id;
    private String name;
    private String description;
    private String version;
    private Map<String, TaskDefinition> tasks = new HashMap<>();
    private List<String> entryPoints = new ArrayList<>();
    private Map<String, Object> defaultContext = new HashMap<>();
    private Map<String, String> metadata = new HashMap<>();
    private long createdAt;
    private long updatedAt;
    private WorkflowStatus status;
    private List<WorkflowTag> tags;
    private List<NodeInstance> nodes;
    private List<Edge> edges;
    private Map<String, Object> globalVariables;
    private List<GuardrailPolicy> guardrails;
    private WorkflowMetadata metadata;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, TaskDefinition> getTasks() {
        return tasks;
    }

    public void setTasks(Map<String, TaskDefinition> tasks) {
        this.tasks = tasks;
    }

    public List<String> getEntryPoints() {
        return entryPoints;
    }

    public void setEntryPoints(List<String> entryPoints) {
        this.entryPoints = entryPoints;
    }

    public Map<String, Object> getDefaultContext() {
        return defaultContext;
    }

    public void setDefaultContext(Map<String, Object> defaultContext) {
        this.defaultContext = defaultContext;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addTask(TaskDefinition task) {
        tasks.put(task.getId(), task);
    }

    public WorkflowStatus getStatus() {
        return this.status;
    }

    public List<WorkflowTag> getTags() {
        return this.tags;
    }
}
