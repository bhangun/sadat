package tech.kayys.wayang.agent.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.sdk.executor.AbstractWorkflowExecutor;
import tech.kayys.silat.sdk.executor.Executor;
import tech.kayys.wayang.agent.model.AgentExecutionResult;
import tech.kayys.wayang.agent.model.AgentConfiguration;
import tech.kayys.wayang.agent.model.AgentContext;
import tech.kayys.wayang.agent.model.LLMRequest;
import tech.kayys.wayang.agent.model.LLMResponse;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.Tool;
import tech.kayys.wayang.agent.model.ToolCall;
import tech.kayys.wayang.agent.model.ToolRegistry;
import tech.kayys.wayang.agent.model.ToolResult;
import tech.kayys.wayang.agent.model.llmprovider.LLMProviderRegistry;
import tech.kayys.silat.execution.DefaultNodeExecutionResult;
import tech.kayys.silat.execution.NodeExecutionResult;
import tech.kayys.silat.execution.NodeExecutionStatus;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.silat.model.CommunicationType;
import tech.kayys.silat.model.ErrorInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main Agent Executor implementation
 * Orchestrates the agent execution lifecycle:
 * 1. Initialize context (memory, tools)
 * 2. Prepare system prompt and messages
 * 3. Call LLM
 * 4. Execute tool calls (if any)
 * 5. Update memory
 * 6. Return result
 */
@Executor(executorType = "common-agent", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 10, version = "1.0.0")
@ApplicationScoped
public class AgentExecutor extends AbstractWorkflowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AgentExecutor.class);

    @Inject
    protected AgentContextManager contextManager;

    @Inject
    protected AgentConfigurationService configurationService;

    @Inject
    protected AgentMetricsCollector metricsCollector;

    @Inject
    protected LLMProviderRegistry llmProviderRegistry;

    @Inject
    protected ToolRegistry toolRegistry;

    @Inject
    protected tech.kayys.wayang.agent.model.AgentMemoryManager memoryManager;

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        LOG.info("Starting agent execution: run={}, node={}, attempt={}",
                task.runId().value(), task.nodeId().value(), task.attempt());

        Instant startTime = Instant.now();

        return loadAgentConfiguration(task)
                // Initialize context
                .flatMap(config -> initializeContext(task, config, getSessionId(task)))
                // Execute agent loop
                .flatMap(context -> executeAgentLoop(task, context))
                // Save state and finalize
                .flatMap(agentResult -> finalizeExecution(task, agentResult, getSessionId(task)))
                .onItem().invoke(result -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    metricsCollector.recordExecution(task.nodeId().value(), duration, true);
                    LOG.info("Agent execution completed: run={}, duration={}ms",
                            task.runId().value(), duration.toMillis());
                })
                .onFailure().invoke(error -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    metricsCollector.recordExecution(task.nodeId().value(), duration, false);
                    LOG.error("Agent execution failed: run={}, error={}",
                            task.runId().value(), error.getMessage(), error);
                })
                .onFailure().recoverWithItem(error -> createFailureResult(task, error));
    }

    /**
     * Load agent configuration from task metadata
     */
    protected Uni<AgentConfiguration> loadAgentConfiguration(NodeExecutionTask task) {
        return Uni.createFrom().deferred(() -> {
            Map<String, Object> metadata = task.context();

            AgentConfiguration.Builder builder = AgentConfiguration.builder()
                    .agentId(task.nodeId().value())
                    .tenantId(extractTenantId(task))
                    .runId(task.runId().value());

            // LLM Configuration
            builder.llmProvider(getStringValue(metadata, "llmProvider", "openai"))
                    .llmModel(getStringValue(metadata, "llmModel", "gpt-4"))
                    .temperature(getDoubleValue(metadata, "temperature", 0.7))
                    .maxTokens(getIntValue(metadata, "maxTokens", 1000));

            // System Prompt
            builder.systemPrompt(getStringValue(metadata, "systemPrompt",
                    "You are a helpful AI assistant."));

            // Tools Configuration
            List<String> tools = getListValue(metadata, "tools");
            builder.enabledTools(tools);
            builder.toolExecutionMode(getStringValue(metadata, "toolExecutionMode", "auto"));

            // Memory Configuration
            boolean memoryEnabled = getBooleanValue(metadata, "memory.enabled", true);
            builder.memoryEnabled(memoryEnabled);
            if (memoryEnabled) {
                builder.memoryType(getStringValue(metadata, "memory.type", "vector"))
                        .memoryWindowSize(getIntValue(metadata, "memory.windowSize", 10));
            }

            // Execution Limits
            int maxIterations = getIntValue(metadata, "maxIterations", 5);
            builder.maxIterations(maxIterations);

            return Uni.createFrom().item(builder.build());
        });
    }

    /**
     * Initialize agent context with memory and available tools
     */
    protected Uni<AgentContext> initializeContext(
            NodeExecutionTask task,
            AgentConfiguration config,
            String sessionId) {

        LOG.debug("Initializing agent context for session: {}", sessionId);

        return Uni.createFrom().deferred(() -> {
            AgentContext context = AgentContext.builder()
                    .sessionId(sessionId)
                    .runId(task.runId().value())
                    .nodeId(task.nodeId().value())
                    .tenantId(extractTenantId(task))
                    .configuration(config)
                    .taskContext(task.context())
                    .build();

            return Uni.createFrom().item(context);
        })
                .flatMap(context -> loadMemoryForSession(context))
                .flatMap(context -> loadAvailableTools(context));
    }

    /**
     * Load conversation memory for the session
     */
    protected Uni<AgentContext> loadMemoryForSession(AgentContext context) {
        if (!context.configuration().memoryEnabled()) {
            LOG.debug("Memory disabled for agent");
            return Uni.createFrom().item(context);
        }

        return memoryManager.loadMemory(
                context.sessionId(),
                context.tenantId(),
                context.configuration().memoryType(),
                context.configuration().memoryWindowSize())
                .map(memory -> {
                    context.setMemory(memory);
                    LOG.debug("Loaded {} memory entries", memory.size());
                    return context;
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.warn("Failed to load memory, continuing without it: {}",
                            error.getMessage());
                    return context;
                });
    }

    /**
     * Load and initialize tools based on configuration
     */
    protected Uni<AgentContext> loadAvailableTools(AgentContext context) {
        if (context.configuration().enabledTools() == null ||
                context.configuration().enabledTools().isEmpty()) {
            LOG.debug("No tools enabled for agent");
            return Uni.createFrom().item(context);
        }

        List<String> toolNames = context.configuration().enabledTools();

        return toolRegistry.getTools(toolNames, context.tenantId())
                .map(tools -> {
                    context.setTools(tools);
                    LOG.debug("Loaded {} tools: {}", tools.size(),
                            tools.stream().map(Tool::name).collect(Collectors.joining(", ")));
                    return context;
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.warn("Failed to load tools, continuing without them: {}",
                            error.getMessage());
                    return context;
                });
    }

    /**
     * Main agent execution loop with tool calling support
     */
    protected Uni<AgentExecutionResult> executeAgentLoop(
            NodeExecutionTask task,
            AgentContext context) {

        return Uni.createFrom().deferred(() -> executeAgentIteration(task, context, 0));
    }

    /**
     * Single iteration of agent execution
     */
    protected Uni<AgentExecutionResult> executeAgentIteration(
            NodeExecutionTask task,
            AgentContext context,
            int iteration) {

        if (iteration >= context.configuration().maxIterations()) {
            LOG.warn("Max iterations reached: {}", iteration);
            return Uni.createFrom().item(
                    AgentExecutionResult.maxIterationsReached(
                            context.getMessages(),
                            iteration));
        }

        LOG.debug("Agent iteration {}/{}",
                iteration + 1, context.configuration().maxIterations());

        return prepareMessages(task, context, iteration)
                .flatMap(messages -> callLLM(context, messages))
                .flatMap(response -> {
                    // Add assistant response to context memory
                    context.addMessage(response.message());

                    // Check if there are tool calls
                    if (response.hasToolCalls()) {
                        LOG.debug("Processing {} tool calls", response.toolCalls().size());

                        return executeToolCalls(context, response.toolCalls())
                                .flatMap(toolResults -> {
                                    // Add tool results to context
                                    for (ToolResult result : toolResults) {
                                        context.addMessage(Message.tool(
                                                result.id(),
                                                result.output()));
                                    }

                                    // Continue to next iteration
                                    return executeAgentIteration(task, context, iteration + 1);
                                });
                    } else {
                        // No tool calls, execution complete
                        return Uni.createFrom().item(
                                AgentExecutionResult.completed(
                                        response,
                                        context.getMessages(),
                                        iteration + 1));
                    }
                });
    }

    /**
     * Prepare messages for LLM call
     */
    protected Uni<List<Message>> prepareMessages(
            NodeExecutionTask task,
            AgentContext context,
            int iteration) {

        return Uni.createFrom().deferred(() -> {
            List<Message> messages = new ArrayList<>();

            // Add system prompt
            if (context.configuration().systemPrompt() != null) {
                messages.add(Message.system(context.configuration().systemPrompt()));
            }

            // Add memory (previous conversation)
            if (context.hasMemory()) {
                messages.addAll(context.getMemory());
            }

            // Add messages from current execution
            messages.addAll(context.getMessages());

            // Add user input if first iteration
            if (iteration == 0 && context.getMessages().isEmpty()) {
                String userInput = extractUserInput(task);
                if (userInput != null) {
                    messages.add(Message.user(userInput));
                }
            }

            LOG.debug("Prepared {} messages for LLM", messages.size());
            return Uni.createFrom().item(messages);
        });
    }

    /**
     * Call LLM provider with messages and tool definitions
     */
    protected Uni<LLMResponse> callLLM(
            AgentContext context,
            List<Message> messages) {

        AgentConfiguration config = context.configuration();

        LLMRequest.Builder requestBuilder = LLMRequest.builder()
                .messages(messages)
                .model(config.llmModel())
                .temperature(config.temperature())
                .maxTokens(config.maxTokens());

        if (context.hasTools()) {
            requestBuilder.tools(context.getTools().stream()
                    .map(Tool::toToolDefinition)
                    .toList());
            requestBuilder.toolChoice(config.toolExecutionMode());
        }

        LLMRequest request = requestBuilder
                .streaming(config.streaming())
                .build();

        LOG.debug("Calling LLM: provider={}, model={}, tools={}",
                config.llmProvider(), config.llmModel(),
                context.hasTools() ? context.getTools().size() : 0);

        return llmProviderRegistry.getProvider(config.llmProvider())
                .flatMap(provider -> provider.complete(request))
                .onItem().invoke(response -> {
                    LOG.debug("LLM response: tokens={}, finish={}",
                            response.usage().totalTokens(),
                            response.finishReason());

                    metricsCollector.recordTokenUsage(
                            config.llmProvider(),
                            config.llmModel(),
                            response.usage());
                });
    }

    /**
     * Execute multiple tool calls in parallel
     */
    protected Uni<List<ToolResult>> executeToolCalls(
            AgentContext context,
            List<ToolCall> toolCalls) {

        List<Uni<ToolResult>> unis = toolCalls.stream()
                .map(call -> executeSingleTool(context, call))
                .collect(Collectors.toList());

        return Uni.join().all(unis).andFailFast()
                .map(results -> results.stream()
                        .map(result -> (ToolResult) result)
                        .collect(Collectors.toList()));
    }

    /**
     * Execute a single tool call
     */
    protected Uni<ToolResult> executeSingleTool(
            AgentContext context,
            ToolCall toolCall) {

        LOG.debug("Executing tool: {} with id: {}",
                toolCall.name(), toolCall.id());

        return toolRegistry.getTool(toolCall.name(), context.tenantId())
                .flatMap(tool -> {
                    if (tool == null) {
                        return Uni.createFrom().item(
                                ToolResult.error(
                                        toolCall.id(),
                                        toolCall.name(),
                                        "Tool not found: " + toolCall.name()));
                    }

                    // Validate tool parameters
                    return tool.validate(toolCall.arguments())
                            .flatMap(isValid -> {
                                if (!isValid) {
                                    return Uni.createFrom().item(
                                            ToolResult.error(
                                                    toolCall.id(),
                                                    toolCall.name(),
                                                    "Invalid tool arguments"));
                                }

                                // Execute tool
                                return tool.execute(toolCall.arguments(), context)
                                        .map(output -> ToolResult.success(
                                                toolCall.id(),
                                                toolCall.name(),
                                                output))
                                        .onFailure().recoverWithItem(error -> ToolResult.error(
                                                toolCall.id(),
                                                toolCall.name(),
                                                error.getMessage()));
                            });
                })
                .onItem().invoke(result -> {
                    LOG.debug("Tool execution completed: {} -> {}",
                            toolCall.name(), result.success() ? "success" : "error");

                    metricsCollector.recordToolExecution(
                            toolCall.name(),
                            result.success());
                });
    }

    /**
     * Finalize execution by saving memory and preparing result
     */
    protected Uni<NodeExecutionResult> finalizeExecution(
            NodeExecutionTask task,
            AgentExecutionResult agentResult,
            String sessionId) {

        return saveMemory(task, agentResult, sessionId)
                .map(v -> createSuccessResult(task, agentResult))
                .onFailure().recoverWithItem(error -> {
                    LOG.warn("Failed to save memory: {}", error.getMessage());
                    // Still return success result even if memory save failed
                    return createSuccessResult(task, agentResult);
                });
    }

    /**
     * Save conversation to memory
     */
    protected Uni<Void> saveMemory(
            NodeExecutionTask task,
            AgentExecutionResult agentResult,
            String sessionId) {

        Map<String, Object> metadata = task.context();
        boolean memoryEnabled = getBooleanValue(metadata, "memory.enabled", true);

        // Also check task config passed down if available, but extracting from metadata
        // check is safer if config object lost
        // Actually we used AgentConfiguration in load... but here we don't have it
        // easily unless passed.
        // We'll trust memory manager or metadata check.

        if (!memoryEnabled) {
            return Uni.createFrom().voidItem();
        }

        List<Message> newMessages = agentResult.messages();
        if (newMessages.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return memoryManager.saveMessages(
                sessionId,
                extractTenantId(task),
                newMessages);
    }

    /**
     * Create success result with agent output
     */
    protected NodeExecutionResult createSuccessResult(
            NodeExecutionTask task,
            AgentExecutionResult agentResult) {

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("response", agentResult.content());
        outputs.put("messages", agentResult.messages());
        outputs.put("iterations", agentResult.iterations());

        // Include tool execution summary if any
        if (agentResult.hadToolCalls()) {
            outputs.put("toolCallsExecuted", agentResult.countToolCalls());
        }

        return new DefaultNodeExecutionResult(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                NodeExecutionStatus.COMPLETED,
                outputs,
                null,
                task.token());
    }

    /**
     * Create failure result from error
     */
    protected NodeExecutionResult createFailureResult(
            NodeExecutionTask task,
            Throwable error) {

        return new DefaultNodeExecutionResult(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                NodeExecutionStatus.FAILED,
                java.util.Collections.emptyMap(),
                new ErrorInfo(
                        "AGENT_EXECUTION_FAILED",
                        error.getMessage() != null ? error.getMessage() : "Unknown error",
                        getStackTrace(error),
                        java.util.Collections.emptyMap()),
                task.token());
    }

    // ==================== HELPER METHODS ====================

    protected String getSessionId(NodeExecutionTask task) {
        return task.context().getOrDefault("sessionId",
                task.runId().value()).toString();
    }

    protected String extractUserInput(NodeExecutionTask task) {
        Object input = task.context().get("input");
        if (input == null) {
            input = task.context().get("prompt");
        }
        if (input == null) {
            input = task.context().get("message");
        }
        return input != null ? input.toString() : null;
    }

    protected String extractTenantId(NodeExecutionTask task) {
        if (task.context().containsKey("tenantId")) {
            return task.context().get("tenantId").toString();
        }
        // Fallback or check token if available
        return "default";
    }

    protected String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = getNestedValue(map, key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    protected Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        Object value = getNestedValue(map, key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    protected Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = getNestedValue(map, key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    protected Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = getNestedValue(map, key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    protected List<String> getListValue(Map<String, Object> map, String key) {
        Object value = getNestedValue(map, key);
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    protected Object getNestedValue(Map<String, Object> map, String key) {
        String[] parts = key.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    protected String getStackTrace(Throwable error) {
        if (error == null)
            return "";

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        String trace = sw.toString();

        // Limit stack trace length
        return trace.length() > 2000 ? trace.substring(0, 2000) + "..." : trace;
    }
}