package tech.kayys.wayang.workflow.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.execution.Variable;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.executor.NodeExecutionResult;

/**
 * Execution context for workflow runtime
 */
public class ExecutionContext {

    private String executionId;
    private String tenantId;
    private WorkflowDefinition workflow;
    private tech.kayys.wayang.workflow.domain.WorkflowRun workflowRun;
    private Map<String, Object> input;
    private Map<String, Object> variables;
    private Set<String> executedNodes;
    private Set<String> executingNodes;
    private List<ExecutionTrace> executionTrace;
    private Map<String, NodeExecutionResult> nodeResults;
    private boolean awaitingHuman = false;
    private long startTime;

    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public ExecutionContext() {
        this.executionId = UUID.randomUUID().toString();
        this.variables = new ConcurrentHashMap<>();
        this.executedNodes = ConcurrentHashMap.newKeySet();
        this.executingNodes = ConcurrentHashMap.newKeySet();
        this.executionTrace = Collections.synchronizedList(new ArrayList<>());
        this.nodeResults = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    public static ExecutionContext create(tech.kayys.wayang.workflow.domain.WorkflowRun run,
            WorkflowDefinition workflow) {
        ExecutionContext context = new ExecutionContext();
        context.executionId = run.getRunId();
        context.workflow = workflow;
        context.setInput(run.getInputs());
        return context;
    }

    /**
     * Initialize variables from workflow definition
     */
    public void initializeVariables(List<Variable> workflowVariables) {
        if (workflowVariables != null) {
            workflowVariables.forEach(var -> {
                if (var.getDefaultValue() != null) {
                    variables.put(var.getName(), var.getDefaultValue());
                }
            });
        }
    }

    /**
     * Set a variable value
     */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * Get a variable value
     */
    public Object getVariable(String name) {
        return variables.get(name);
    }

    /**
     * Resolve variable by path (supports nested access like "user.name")
     */
    public Object resolveVariable(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = variables.get(parts[0]);

        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                // Try to access as property using reflection
                try {
                    current = current.getClass()
                            .getMethod("get" + capitalize(parts[i]))
                            .invoke(current);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        return current;
    }

    /**
     * Get all variables
     */
    public Map<String, Object> getAllVariables() {
        return new HashMap<>(variables);
    }

    /**
     * Interpolate template with variables
     */
    public String interpolateTemplate(String template, List<String> variableNames) {
        String result = template;

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = resolveVariable(varName);
            if (value != null) {
                result = result.replace("${" + varName + "}", value.toString());
            }
        }

        return result;
    }

    /**
     * Evaluate expression
     */
    public boolean evaluateExpression(String expression, Map<String, Object> context) {
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("nashorn");
            if (engine == null) {
                // Try alternative JavaScript engine name (for older Java versions)
                engine = scriptEngineManager.getEngineByName("javascript");
            }

            if (engine == null) {
                // No JavaScript engine available - for simple cases, return true if expression
                // is not empty
                // This is a fallback implementation when script engine is not available
                return !expression.trim().isEmpty()
                        && !("false".equals(expression.trim()) || "0".equals(expression.trim()));
            }

            // Add context variables to engine
            context.forEach(engine::put);
            variables.forEach(engine::put);

            // Evaluate expression
            Object result = engine.eval(expression);

            if (result instanceof Boolean) {
                return (Boolean) result;
            }

            // Convert to boolean
            return result != null && !result.equals(0) && !result.equals("");

        } catch (Exception e) { // Catch broader exception since javax.script might not exist
            // Fallback implementation for when script engine is not available
            return !expression.trim().isEmpty()
                    && !("false".equals(expression.trim()) || "0".equals(expression.trim()));
        }
    }

    /**
     * Mark node as executing
     */
    public void markNodeExecuting(String nodeId) {
        executingNodes.add(nodeId);
    }

    /**
     * Mark node as executed
     */
    public void markNodeExecuted(String nodeId, NodeExecutionResult result) {
        executingNodes.remove(nodeId);
        executedNodes.add(nodeId);
        nodeResults.put(nodeId, result);

        // Add to trace
        executionTrace.add(new ExecutionTrace(
                nodeId,
                getNodeName(nodeId),
                System.currentTimeMillis(),
                new HashMap<>(),
                result.getOutput(),
                result.isSuccess() ? "success" : "failed"));
    }

    /**
     * Check if node is executed
     */
    public boolean isNodeExecuted(String nodeId) {
        return executedNodes.contains(nodeId);
    }

    /**
     * Check if node is executing
     */
    public boolean isNodeExecuting(String nodeId) {
        return executingNodes.contains(nodeId);
    }

    /**
     * Get node result
     */
    public NodeExecutionResult getNodeResult(String nodeId) {
        return nodeResults.get(nodeId);
    }

    /**
     * Get node name from workflow
     */
    private String getNodeName(String nodeId) {
        if (workflow != null && workflow.getNodes() != null) {
            for (tech.kayys.wayang.schema.node.NodeDefinition node : workflow.getNodes()) {
                if (node != null && nodeId.equals(node.getId())) {
                    return node.getDisplayName() != null ? node.getDisplayName() : nodeId;
                }
            }
        }
        return nodeId;
    }

    /**
     * Get execution duration
     */
    public long getExecutionDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Getters and Setters
    public String getExecutionId() {
        return executionId;
    }

    public WorkflowDefinition getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowDefinition workflow) {
        this.workflow = workflow;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
        // Add input to variables
        if (input != null) {
            this.variables.putAll(input);
        }
    }

    public List<ExecutionTrace> getExecutionTrace() {
        return new ArrayList<>(executionTrace);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public tech.kayys.wayang.workflow.domain.WorkflowRun getWorkflowRun() {
        return workflowRun;
    }

    public void setWorkflowRun(tech.kayys.wayang.workflow.domain.WorkflowRun workflowRun) {
        this.workflowRun = workflowRun;
    }

    public boolean isAwaitingHuman() {
        return awaitingHuman;
    }

    public void setAwaitingHuman(boolean awaitingHuman) {
        this.awaitingHuman = awaitingHuman;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
}
