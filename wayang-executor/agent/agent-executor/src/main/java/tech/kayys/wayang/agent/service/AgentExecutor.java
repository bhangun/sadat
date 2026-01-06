

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.agent.core.*;
import tech.kayys.silat.agent.memory.*;
import tech.kayys.silat.agent.model.*;
import tech.kayys.silat.agent.tools.*;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.executor.AbstractWorkflowExecutor;
import tech.kayys.silat.executor.Executor;
import tech.kayys.silat.core.scheduler.CommunicationType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * SILAT COMMON AGENT EXECUTOR
 * ============================================================================
 * 
 * A sophisticated agent executor that integrates:
 * - Multi-provider LLM support (OpenAI, Anthropic, Azure, Local models)
 * - Pluggable memory systems (Short-term, Long-term, Vector-based)
 * - Tool/Function calling capabilities
 * - Structured output support
 * - Streaming responses
 * - Context management
 * - Error recovery and retry logic
 * 
 * Architecture:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    Agent Executor                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  ┌───────────┐  ┌──────────┐  ┌─────────┐  ┌──────────┐   │
 * │  │  Memory   │  │   LLM    │  │  Tools  │  │ Context  │   │
 * │  │  Manager  │  │ Provider │  │Registry │  │ Manager  │   │
 * │  └───────────┘  └──────────┘  └─────────┘  └──────────┘   │
 * │       │              │              │            │          │
 * │       └──────────────┴──────────────┴────────────┘          │
 * │                      │                                       │
 * │              ┌───────▼────────┐                             │
 * │              │ Execution Flow │                             │
 * │              └────────────────┘                             │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * Usage Example:
 * ```java
 * @Executor(
 *     executorType = "common-agent",
 *     communicationType = CommunicationType.GRPC
 * )
 * public class MyAgentExecutor extends AgentExecutor {
 *     // Custom initialization if needed
 * }
 * ```
 */
@Executor(
    executorType = "common-agent",
    communicationType = CommunicationType.GRPC,
    maxConcurrentTasks = 10,
    version = "1.0.0"
)
@ApplicationScoped
public class AgentExecutor extends AbstractWorkflowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AgentExecutor.class);

    @Inject
    protected AgentMemoryManager memoryManager;

    @Inject
    protected LLMProviderRegistry llmRegistry;

    @Inject
    protected ToolRegistry toolRegistry;

    @Inject
    protected AgentContextManager contextManager;

    @Inject
    protected AgentConfigurationService configService;

    @Inject
    protected AgentMetricsCollector metricsCollector;

    /**
     * Main execution entry point for agent tasks
     * 
     * Execution Flow:
     * 1. Load agent configuration from node metadata
     * 2. Initialize context with memory and tools
     * 3. Prepare prompt with system instructions and user input
     * 4. Execute LLM call with tool support
     * 5. Process tool calls if any
     * 6. Update memory with interaction
     * 7. Return result
     */
    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        LOG.info("Starting agent execution: run={}, node={}, attempt={}", 
            task.runId().value(), task.nodeId().value(), task.attempt());

        Instant startTime = Instant.now();
        String sessionId = extractSessionId(task);

        return loadAgentConfiguration(task)
            .flatMap(config -> initializeAgentContext(task, config, sessionId))
            .flatMap(context -> executeAgentLoop(task, context))
            .flatMap(result -> finalizeExecution(task, result, sessionId))
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
            .onFailure().recoverWithItem(error -> 
                createFailureResult(task, error)
            );
    }

    /**
     * Load agent configuration from task metadata
     * Configuration includes:
     * - LLM provider and model
     * - Memory settings
     * - Available tools
     * - System prompt
     * - Temperature and other parameters
     */
    protected Uni<AgentConfiguration> loadAgentConfiguration(NodeExecutionTask task) {
        return Uni.createFrom().deferred(() -> {
            Map<String, Object> metadata = task.metadata();
            
            AgentConfiguration.Builder builder = AgentConfiguration.builder()
                .agentId(task.nodeId().value())
                .tenantId(task.tenantId())
                .runId(task.runId().value());

            // LLM Configuration
            String provider = getStringValue(metadata, "llm.provider", "openai");
            String model = getStringValue(metadata, "llm.model", "gpt-4");
            Double temperature = getDoubleValue(metadata, "llm.temperature", 0.7);
            Integer maxTokens = getIntValue(metadata, "llm.maxTokens", 2000);
            
            builder.llmProvider(provider)
                   .llmModel(model)
                   .temperature(temperature)
                   .maxTokens(maxTokens);

            // Memory Configuration
            boolean enableMemory = getBooleanValue(metadata, "memory.enabled", true);
            String memoryType = getStringValue(metadata, "memory.type", "buffer");
            Integer memoryWindowSize = getIntValue(metadata, "memory.windowSize", 10);
            
            builder.memoryEnabled(enableMemory)
                   .memoryType(memoryType)
                   .memoryWindowSize(memoryWindowSize);

            // Tools Configuration
            List<String> enabledTools = getListValue(metadata, "tools.enabled");
            boolean allowToolCalls = getBooleanValue(metadata, "tools.allowCalls", true);
            
            builder.enabledTools(enabledTools)
                   .allowToolCalls(allowToolCalls);

            // System Prompt
            String systemPrompt = getStringValue(metadata, "systemPrompt", 
                "You are a helpful AI assistant.");
            builder.systemPrompt(systemPrompt);

            // Streaming
            boolean streaming = getBooleanValue(metadata, "streaming", false);
            builder.streaming(streaming);

            // Max iterations for tool loops
            Integer maxIterations = getIntValue(metadata, "maxIterations", 5);
            builder.maxIterations(maxIterations);

            return Uni.createFrom().item(builder.build());
        });
    }

    /**
     * Initialize agent context with memory and available tools
     */
    protected Uni<AgentContext> initializeAgentContext(
            NodeExecutionTask task,
            AgentConfiguration config,
            String sessionId) {
        
        LOG.debug("Initializing agent context for session: {}", sessionId);

        return Uni.createFrom().deferred(() -> {
            AgentContext context = AgentContext.builder()
                .sessionId(sessionId)
                .runId(task.runId().value())
                .nodeId(task.nodeId().value())
                .tenantId(task.tenantId())
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
                context.configuration().memoryWindowSize()
            )
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
        if (!context.configuration().allowToolCalls() || 
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
     * 
     * Flow:
     * 1. Prepare messages with memory and context
     * 2. Call LLM
     * 3. If tool calls present, execute them
     * 4. Add tool results to messages
     * 5. Call LLM again with tool results
     * 6. Repeat until no more tool calls or max iterations
     */
    protected Uni<AgentExecutionResult> executeAgentLoop(
            NodeExecutionTask task,
            AgentContext context) {
        
        return Uni.createFrom().deferred(() -> 
            executeAgentIteration(task, context, 0)
        );
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
                    iteration
                )
            );
        }

        LOG.debug("Agent iteration {}/{}", 
            iteration + 1, context.configuration().maxIterations());

        return prepareMessages(task, context, iteration)
            .flatMap(messages -> callLLM(context, messages))
            .flatMap(response -> {
                // Add assistant response to context
                context.addMessage(response);

                // Check if there are tool calls
                if (response.hasToolCalls()) {
                    LOG.debug("Processing {} tool calls", response.toolCalls().size());
                    
                    return executeToolCalls(context, response.toolCalls())
                        .flatMap(toolResults -> {
                            // Add tool results to context
                            toolResults.forEach(context::addToolResult);
                            
                            // Continue to next iteration
                            return executeAgentIteration(task, context, iteration + 1);
                        });
                } else {
                    // No tool calls, execution complete
                    return Uni.createFrom().item(
                        AgentExecutionResult.completed(
                            response,
                            context.getMessages(),
                            iteration + 1
                        )
                    );
                }
            });
    }

    /**
     * Prepare messages for LLM call
     * Includes: system prompt, memory, user input, tool results
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

            // If first iteration, add user input
            if (iteration == 0) {
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
        
        LLMRequest request = LLMRequest.builder()
            .provider(config.llmProvider())
            .model(config.llmModel())
            .messages(messages)
            .temperature(config.temperature())
            .maxTokens(config.maxTokens())
            .tools(context.hasTools() ? 
                context.getTools().stream()
                    .map(Tool::toToolDefinition)
                    .collect(Collectors.toList()) : 
                List.of())
            .streaming(config.streaming())
            .build();

        LOG.debug("Calling LLM: provider={}, model={}, tools={}", 
            config.llmProvider(), config.llmModel(), 
            context.hasTools() ? context.getTools().size() : 0);

        return llmRegistry.getProvider(config.llmProvider())
            .flatMap(provider -> provider.complete(request))
            .onItem().invoke(response -> {
                LOG.debug("LLM response: tokens={}, finish={}", 
                    response.usage().totalTokens(),
                    response.finishReason());
                
                metricsCollector.recordTokenUsage(
                    config.llmProvider(),
                    config.llmModel(),
                    response.usage()
                );
            });
    }

    /**
     * Execute multiple tool calls in parallel
     */
    protected Uni<List<ToolResult>> executeToolCalls(
            AgentContext context,
            List<ToolCall> toolCalls) {
        
        List<Uni<ToolResult>> toolExecutions = toolCalls.stream()
            .map(toolCall -> executeSingleToolCall(context, toolCall))
            .collect(Collectors.toList());

        return Uni.combine().all().unis(toolExecutions).combinedWith(
            results -> results.stream()
                .map(result -> (ToolResult) result)
                .collect(Collectors.toList())
        );
    }

    /**
     * Execute a single tool call
     */
    protected Uni<ToolResult> executeSingleToolCall(
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
                            "Tool not found: " + toolCall.name()
                        )
                    );
                }

                // Validate tool parameters
                return tool.validate(toolCall.arguments())
                    .flatMap(valid -> {
                        if (!valid) {
                            return Uni.createFrom().item(
                                ToolResult.error(
                                    toolCall.id(),
                                    toolCall.name(),
                                    "Invalid tool arguments"
                                )
                            );
                        }

                        // Execute tool
                        return tool.execute(toolCall.arguments(), context)
                            .map(output -> ToolResult.success(
                                toolCall.id(),
                                toolCall.name(),
                                output
                            ))
                            .onFailure().recoverWithItem(error -> 
                                ToolResult.error(
                                    toolCall.id(),
                                    toolCall.name(),
                                    error.getMessage()
                                )
                            );
                    });
            })
            .onItem().invoke(result -> {
                LOG.debug("Tool execution completed: {} -> {}", 
                    toolCall.name(), result.success() ? "success" : "error");
                
                metricsCollector.recordToolExecution(
                    toolCall.name(),
                    result.success()
                );
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
        
        AgentConfiguration config = agentResult.messages().isEmpty() ? null :
            AgentConfiguration.builder().build(); // Get from context
        
        if (config != null && !config.memoryEnabled()) {
            return Uni.createFrom().voidItem();
        }

        List<Message> newMessages = agentResult.messages();
        if (newMessages.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return memoryManager.saveMessages(
            sessionId,
            task.tenantId(),
            newMessages
        );
    }

    /**
     * Create success result with agent output
     */
    protected NodeExecutionResult createSuccessResult(
            NodeExecutionTask task,
            AgentExecutionResult agentResult) {
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("response", agentResult.finalResponse().content());
        outputs.put("iterations", agentResult.iterations());
        outputs.put("finishReason", agentResult.finalResponse().finishReason());
        outputs.put("tokenUsage", agentResult.finalResponse().usage().toMap());
        
        // Include tool execution summary if any
        if (agentResult.hadToolCalls()) {
            outputs.put("toolCallsExecuted", agentResult.countToolCalls());
        }

        return NodeExecutionResult.success(
            task.runId(),
            task.nodeId(),
            task.attempt(),
            outputs,
            task.token()
        );
    }

    /**
     * Create failure result from error
     */
    protected NodeExecutionResult createFailureResult(
            NodeExecutionTask task,
            Throwable error) {
        
        return NodeExecutionResult.failure(
            task.runId(),
            task.nodeId(),
            task.attempt(),
            new ErrorInfo(
                "AGENT_EXECUTION_FAILED",
                error.getMessage(),
                error.getClass().getSimpleName(),
                Map.of("stackTrace", getStackTrace(error))
            ),
            task.token()
        );
    }

    // ==================== HELPER METHODS ====================

    protected String extractSessionId(NodeExecutionTask task) {
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

    protected String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = getNestedValue(map, key);
        return value != null ? value.toString() : defaultValue;
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

    @SuppressWarnings("unchecked")
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
        if (error == null) return "";
        
        java.io.StringWriter sw = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(sw));
        String trace = sw.toString();
        
        // Limit stack trace length
        return trace.length() > 2000 ? trace.substring(0, 2000) + "..." : trace;
    }
}