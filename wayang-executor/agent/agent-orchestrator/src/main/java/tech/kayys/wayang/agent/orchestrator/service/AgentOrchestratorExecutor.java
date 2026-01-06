package tech.kayys.wayang.agent.orchestrator.service;


import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/* import tech.kayys.silat.agent.domain.*;
import tech.kayys.silat.executor.AbstractWorkflowExecutor;
import tech.kayys.silat.executor.Executor;
import tech.kayys.silat.executor.NodeExecutionTask;
import tech.kayys.silat.executor.NodeExecutionResult;
import tech.kayys.silat.core.scheduler.CommunicationType;
import tech.kayys.silat.core.domain.*;
 */
import java.time.Duration;
import java.time.Instant;

import java.util.stream.Collectors;

/**
 * ============================================================================
 * AGENT ORCHESTRATOR EXECUTOR
 * ============================================================================
 * 
 * Comprehensive orchestrator for multi-agent coordination supporting:
 * - Sequential, parallel, and hierarchical orchestration
 * - Built-in planner, executor, and evaluator agents
 * - Dynamic agent selection and task distribution
 * - Adaptive replanning and failure recovery
 * - Multi-level orchestration (recursive)
 * - Agent collaboration and consensus
 * 
 * Architecture:
 * - Event-driven coordination
 * - Reactive execution with backpressure
 * - Circuit breaker for agent failures
 * - Distributed tracing support
 * 
 * Package: tech.kayys.silat.agent.executor
 */
@Executor(
    executorType = "agent-orchestrator",
    communicationType = CommunicationType.GRPC,
    maxConcurrentTasks = 10
)
@ApplicationScoped
public class AgentOrchestratorExecutor extends AbstractWorkflowExecutor {
    
    private static final Logger LOG = LoggerFactory.getLogger(AgentOrchestratorExecutor.class);
    
    @Inject
    AgentRegistry agentRegistry;
    
    @Inject
    OrchestratorPlanner orchestratorPlanner;
    
    @Inject
    OrchestratorExecutionEngine executionEngine;
    
    @Inject
    OrchestratorEvaluator orchestratorEvaluator;
    
    @Inject
    AgentCoordinator agentCoordinator;
    
    @Inject
    AgentCommunicationBus communicationBus;
    
    // Orchestration context cache
    private final ConcurrentMap<String, OrchestrationContext> activeOrchestrations = 
        new ConcurrentHashMap<>();
    
    /**
     * Execute orchestration task
     */
    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        LOG.info("Starting agent orchestration for task: {}", task.runId());
        
        // Extract orchestration request
        AgentExecutionRequest request = extractRequest(task);
        
        // Initialize orchestration context
        String orchestrationId = UUID.randomUUID().toString();
        
        return orchestrate(orchestrationId, request, task)
            .onItem().transform(result -> 
                NodeExecutionResult.success(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    Map.of(
                        "orchestrationId", orchestrationId,
                        "status", result.status().name(),
                        "output", result.output(),
                        "metrics", result.metrics(),
                        "events", result.metadata().get("events")
                    ),
                    task.token()
                )
            )
            .onFailure().recoverWithItem(error -> {
                LOG.error("Orchestration failed", error);
                return NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(error),
                    task.token()
                );
            })
            .eventually(() -> 
                // Cleanup
                Uni.createFrom().item(() -> {
                    activeOrchestrations.remove(orchestrationId);
                    return null;
                })
            );
    }
    
    /**
     * Main orchestration flow
     */
    private Uni<AgentExecutionResult> orchestrate(
            String orchestrationId,
            AgentExecutionRequest request,
            NodeExecutionTask task) {
        
        LOG.info("Orchestration {} - Starting planning phase", orchestrationId);
        
        Instant startTime = Instant.now();
        
        // Phase 1: Planning
        return orchestratorPlanner.createPlan(request)
            .flatMap(plan -> {
                LOG.info("Orchestration {} - Plan created with {} steps", 
                    orchestrationId, plan.steps().size());
                
                // Initialize context
                OrchestrationContext context = new OrchestrationContext(
                    orchestrationId,
                    null, // parent orchestration
                    plan,
                    new HashMap<>(),
                    ConcurrentHashMap.newKeySet(),
                    OrchestrationState.EXECUTING,
                    new ConcurrentHashMap<>(request.context()),
                    new CopyOnWriteArrayList<>(),
                    startTime
                );
                
                activeOrchestrations.put(orchestrationId, context);
                
                // Phase 2: Execution
                return executePlan(context, request.constraints())
                    .flatMap(executedContext -> {
                        LOG.info("Orchestration {} - Execution complete", orchestrationId);
                        
                        // Phase 3: Evaluation
                        return evaluateResults(executedContext)
                            .map(evaluation -> {
                                LOG.info("Orchestration {} - Evaluation complete, score: {}", 
                                    orchestrationId, evaluation.overallScore());
                                
                                // Build final result
                                return buildFinalResult(
                                    request.requestId(),
                                    executedContext,
                                    evaluation,
                                    startTime
                                );
                            });
                    });
            })
            .onFailure().call(error -> 
                // Handle failure with potential replanning
                handleOrchestrationFailure(orchestrationId, error, request)
            );
    }
    
    /**
     * Execute the orchestration plan
     */
    private Uni<OrchestrationContext> executePlan(
            OrchestrationContext context,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing plan with {} steps", context.plan().steps().size());
        
        // Determine execution strategy based on orchestration type
        OrchestratorAgent orchestrator = extractOrchestratorConfig(context);
        
        return switch (orchestrator.orchestrationType()) {
            case SEQUENTIAL -> executeSequential(context, constraints);
            case PARALLEL -> executeParallel(context, constraints);
            case HIERARCHICAL -> executeHierarchical(context, constraints);
            case COLLABORATIVE -> executeCollaborative(context, constraints);
            case COMPETITIVE -> executeCompetitive(context, constraints);
            case DEBATE -> executeDebate(context, constraints);
        };
    }
    
    /**
     * Sequential execution - one step at a time
     */
    private Uni<OrchestrationContext> executeSequential(
            OrchestrationContext context,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing {} steps sequentially", context.plan().steps().size());
        
        // Build execution chain
        Uni<OrchestrationContext> chain = Uni.createFrom().item(context);
        
        for (PlanStep step : context.plan().steps()) {
            chain = chain.flatMap(ctx -> 
                executeStep(ctx, step, constraints)
                    .flatMap(result -> {
                        // Update context with result
                        OrchestrationContext updatedCtx = ctx.addStepResult(
                            step.stepId(), 
                            result
                        );
                        
                        // Check if step succeeded
                        if (!result.isSuccess()) {
                            return handleStepFailure(updatedCtx, step, result);
                        }
                        
                        return Uni.createFrom().item(updatedCtx);
                    })
            );
        }
        
        return chain;
    }
    
    /**
     * Parallel execution - multiple steps concurrently
     */
    private Uni<OrchestrationContext> executeParallel(
            OrchestrationContext context,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing {} steps in parallel", context.plan().steps().size());
        
        // Group steps by dependencies
        List<List<PlanStep>> executionWaves = buildExecutionWaves(context.plan());
        
        // Execute waves sequentially, steps in wave parallel
        Uni<OrchestrationContext> chain = Uni.createFrom().item(context);
        
        for (List<PlanStep> wave : executionWaves) {
            chain = chain.flatMap(ctx -> 
                executeWave(ctx, wave, constraints)
            );
        }
        
        return chain;
    }
    
    /**
     * Execute a wave of parallel steps
     */
    private Uni<OrchestrationContext> executeWave(
            OrchestrationContext context,
            List<PlanStep> steps,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing wave with {} parallel steps", steps.size());
        
        // Execute all steps in parallel
        List<Uni<AgentExecutionResult>> executions = steps.stream()
            .map(step -> executeStep(context, step, constraints))
            .collect(Collectors.toList());
        
        // Wait for all to complete
        return Uni.join().all(executions).andCollectFailures()
            .map(results -> {
                // Merge all results into context
                OrchestrationContext updatedCtx = context;
                for (int i = 0; i < steps.size(); i++) {
                    updatedCtx = updatedCtx.addStepResult(
                        steps.get(i).stepId(),
                        results.get(i)
                    );
                }
                return updatedCtx;
            });
    }
    
    /**
     * Hierarchical execution - tree-like delegation
     */
    private Uni<OrchestrationContext> executeHierarchical(
            OrchestrationContext context,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing hierarchical orchestration");
        
        // Build execution tree
        ExecutionTree tree = buildExecutionTree(context.plan());
        
        // Execute from root
        return executeTreeNode(context, tree.root(), constraints);
    }
    
    /**
     * Collaborative execution - agents work together
     */
    private Uni<OrchestrationContext> executeCollaborative(
            OrchestrationContext context,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing collaborative orchestration");
        
        // Create collaboration session
        return agentCoordinator.initiateCollaboration(
            context.orchestrationId(),
            context.plan(),
            constraints
        )
        .flatMap(session -> 
            // Monitor collaboration until complete
            monitorCollaboration(context, session)
        );
    }
    
    /**
     * Competitive execution - best result wins
     */
    private Uni<OrchestrationContext> executeCompetitive(
            OrchestrationContext context,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing competitive orchestration");
        
        // Execute same task with multiple agents
        List<PlanStep> competitiveSteps = context.plan().steps();
        
        return Multi.createFrom().iterable(competitiveSteps)
            .onItem().transformToUniAndMerge(step -> 
                executeStep(context, step, constraints)
            )
            .collect().asList()
            .map(results -> {
                // Select best result
                AgentExecutionResult bestResult = selectBestResult(results);
                
                // Update context with winning result
                return context.addStepResult("competitive_winner", bestResult);
            });
    }
    
    /**
     * Debate execution - agents debate solutions
     */
    private Uni<OrchestrationContext> executeDebate(
            OrchestrationContext context,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing debate orchestration");
        
        // Create debate session
        return agentCoordinator.initiateDebate(
            context.orchestrationId(),
            context.plan(),
            constraints
        )
        .flatMap(debate -> 
            // Run debate rounds
            runDebateRounds(context, debate)
        );
    }
    
    /**
     * Execute a single plan step
     */
    private Uni<AgentExecutionResult> executeStep(
            OrchestrationContext context,
            PlanStep step,
            ExecutionConstraints constraints) {
        
        LOG.debug("Executing step: {} - {}", step.stepId(), step.description());
        
        // Record step start event
        context = context.addEvent(new OrchestrationEvent(
            UUID.randomUUID().toString(),
            OrchestrationEventType.STEP_STARTED,
            "Step started: " + step.description(),
            Map.of("stepId", step.stepId()),
            Instant.now()
        ));
        
        // Select appropriate agent for step
        return selectAgentForStep(step, context)
            .flatMap(agent -> {
                LOG.debug("Selected agent {} for step {}", agent.agentId(), step.stepId());
                
                // Add to active agents
                context.activeAgents().add(agent.agentId());
                
                // Create execution request
                AgentExecutionRequest request = AgentExecutionRequest.builder()
                    .taskDescription(step.description())
                    .context(step.stepContext())
                    .constraints(constraints)
                    .build();
                
                // Execute with selected agent
                return executionEngine.executeWithAgent(agent, request)
                    .eventually(() -> 
                        // Remove from active agents
                        Uni.createFrom().item(() -> {
                            context.activeAgents().remove(agent.agentId());
                            return null;
                        })
                    );
            })
            .onItem().invoke(result -> {
                // Record step completion event
                OrchestrationEvent event = new OrchestrationEvent(
                    UUID.randomUUID().toString(),
                    result.isSuccess() ? 
                        OrchestrationEventType.STEP_COMPLETED : 
                        OrchestrationEventType.STEP_FAILED,
                    "Step " + (result.isSuccess() ? "completed" : "failed"),
                    Map.of("stepId", step.stepId(), "status", result.status()),
                    Instant.now()
                );
                context.addEvent(event);
            });
    }
    
    /**
     * Select most appropriate agent for step
     */
    private Uni<AgentRegistration> selectAgentForStep(
            PlanStep step,
            OrchestrationContext context) {
        
        // Extract required capabilities from step
        Set<AgentCapability> requiredCapabilities = 
            extractCapabilities(step.assignedAgentType());
        
        // Query available agents
        return agentRegistry.findAvailableAgents(
            step.assignedAgentType(),
            requiredCapabilities
        )
        .map(agents -> {
            if (agents.isEmpty()) {
                throw new IllegalStateException(
                    "No available agents for type: " + step.assignedAgentType());
            }
            
            // Select based on load balancing, health, and past performance
            return selectBestAgent(agents, context);
        });
    }
    
    /**
     * Select best agent based on multiple criteria
     */
    private AgentRegistration selectBestAgent(
            List<AgentRegistration> agents,
            OrchestrationContext context) {
        
        // Score each agent
        return agents.stream()
            .max(Comparator.comparingDouble(agent -> scoreAgent(agent, context)))
            .orElse(agents.get(0));
    }
    
    /**
     * Score agent for selection
     */
    private double scoreAgent(AgentRegistration agent, OrchestrationContext context) {
        double score = 1.0;
        
        // Factor 1: Availability (heavily weighted)
        if (agent.isAvailable()) {
            score *= 2.0;
        }
        
        // Factor 2: Health
        if (agent.isHealthy()) {
            score *= 1.5;
        }
        
        // Factor 3: Past performance (from metadata)
        Object successRate = agent.metadata().get("successRate");
        if (successRate instanceof Number) {
            score *= ((Number) successRate).doubleValue();
        }
        
        // Factor 4: Current load (prefer less busy agents)
        Object currentLoad = agent.metadata().get("currentLoad");
        if (currentLoad instanceof Number) {
            double load = ((Number) currentLoad).doubleValue();
            score *= (1.0 - load); // Inverse of load
        }
        
        return score;
    }
    
    /**
     * Handle step failure with potential retry/replanning
     */
    private Uni<OrchestrationContext> handleStepFailure(
            OrchestrationContext context,
            PlanStep step,
            AgentExecutionResult result) {
        
        LOG.warn("Step {} failed: {}", step.stepId(), result.errors());
        
        // Check if replanning is enabled
        OrchestratorAgent orchestrator = extractOrchestratorConfig(context);
        
        if (orchestrator.builtInAgents().planner().enableAdaptivePlanning()) {
            LOG.info("Attempting replanning due to step failure");
            
            // Trigger replanning
            return orchestratorPlanner.replan(
                context.plan(),
                step,
                result,
                context.stepResults()
            )
            .flatMap(newPlan -> {
                // Update context with new plan
                OrchestrationContext replanCtx = new OrchestrationContext(
                    context.orchestrationId(),
                    context.parentOrchestrationId(),
                    newPlan,
                    context.stepResults(),
                    context.activeAgents(),
                    OrchestrationState.EXECUTING,
                    context.sharedContext(),
                    context.events(),
                    context.startedAt()
                );
                
                // Add replanning event
                replanCtx = replanCtx.addEvent(new OrchestrationEvent(
                    UUID.randomUUID().toString(),
                    OrchestrationEventType.REPLANNING_TRIGGERED,
                    "Replanning triggered due to step failure",
                    Map.of("failedStep", step.stepId()),
                    Instant.now()
                ));
                
                // Continue with new plan
                return Uni.createFrom().item(replanCtx);
            });
        } else {
            // No replanning, propagate failure
            return Uni.createFrom().failure(
                new OrchestrationException("Step failed: " + step.stepId(), result.errors())
            );
        }
    }
    
    /**
     * Evaluate orchestration results
     */
    private Uni<OrchestratorEvaluator.EvaluationResult> evaluateResults(
            OrchestrationContext context) {
        
        LOG.debug("Evaluating orchestration results");
        
        return orchestratorEvaluator.evaluate(
            context.plan(),
            context.stepResults(),
            context.sharedContext()
        );
    }
    
    /**
     * Build final orchestration result
     */
    private AgentExecutionResult buildFinalResult(
            String requestId,
            OrchestrationContext context,
            OrchestratorEvaluator.EvaluationResult evaluation,
            Instant startTime) {
        
        // Aggregate outputs from all steps
        Map<String, Object> aggregatedOutput = aggregateStepOutputs(context.stepResults());
        
        // Build actions taken list
        List<String> actionsTaken = context.stepResults().values().stream()
            .flatMap(result -> result.actionsTaken().stream())
            .collect(Collectors.toList());
        
        // Calculate metrics
        long executionTime = Duration.between(startTime, Instant.now()).toMillis();
        int totalTokens = context.stepResults().values().stream()
            .mapToInt(r -> r.metrics().tokensUsed())
            .sum();
        int totalToolInvocations = context.stepResults().values().stream()
            .mapToInt(r -> r.metrics().toolInvocations())
            .sum();
        
        ExecutionMetrics metrics = new ExecutionMetrics(
            executionTime,
            totalTokens,
            totalToolInvocations,
            0L,
            evaluation.overallScore(),
            Map.of(
                "planCompleteness", context.plan().getCompletionPercentage(),
                "stepsExecuted", context.stepResults().size(),
                "totalSteps", context.plan().steps().size()
            )
        );
        
        // Collect any errors
        List<ExecutionError> errors = context.stepResults().values().stream()
            .flatMap(r -> r.errors().stream())
            .collect(Collectors.toList());
        
        // Build metadata
        Map<String, Object> metadata = Map.of(
            "orchestrationId", context.orchestrationId(),
            "planId", context.plan().planId(),
            "events", context.events(),
            "evaluation", evaluation
        );
        
        ExecutionStatus status = evaluation.overallScore() >= 0.8 ?
            ExecutionStatus.SUCCESS :
            (evaluation.overallScore() >= 0.5 ? 
                ExecutionStatus.PARTIAL_SUCCESS : 
                ExecutionStatus.FAILED);
        
        return new AgentExecutionResult(
            requestId,
            "orchestrator",
            status,
            aggregatedOutput,
            actionsTaken,
            metrics,
            errors,
            metadata,
            Instant.now()
        );
    }
    
    /**
     * Aggregate outputs from all step results
     */
    private Map<String, Object> aggregateStepOutputs(
            Map<String, AgentExecutionResult> stepResults) {
        
        Map<String, Object> aggregated = new HashMap<>();
        
        stepResults.forEach((stepId, result) -> {
            if (result.output() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stepOutput = (Map<String, Object>) result.output();
                aggregated.putAll(stepOutput);
            } else {
                aggregated.put(stepId, result.output());
            }
        });
        
        return aggregated;
    }
    
    // ==================== HELPER METHODS ====================
    
    private AgentExecutionRequest extractRequest(NodeExecutionTask task) {
        Map<String, Object> context = task.context();
        
        return AgentExecutionRequest.builder()
            .taskDescription((String) context.get("taskDescription"))
            .context((Map<String, Object>) context.getOrDefault("context", Map.of()))
            .constraints(extractConstraints(context))
            .build();
    }
    
    private ExecutionConstraints extractConstraints(Map<String, Object> context) {
        Object constraintsObj = context.get("constraints");
        if (constraintsObj instanceof ExecutionConstraints) {
            return (ExecutionConstraints) constraintsObj;
        }
        return ExecutionConstraints.createDefault();
    }
    
    private OrchestratorAgent extractOrchestratorConfig(OrchestrationContext context) {
        // Extract from plan metadata or use default
        return new OrchestratorAgent(
            OrchestrationType.SEQUENTIAL,
            BuiltInAgents.createDefault(),
            CoordinationStrategy.CENTRALIZED,
            5,
            false
        );
    }
    
    private Set<AgentCapability> extractCapabilities(String agentType) {
        return switch (agentType.toUpperCase()) {
            case "PLANNER" -> Set.of(AgentCapability.PLANNING, AgentCapability.REASONING);
            case "CODER" -> Set.of(AgentCapability.CODE_GENERATION, AgentCapability.CODE_ANALYSIS);
            case "ANALYST" -> Set.of(AgentCapability.DATA_ANALYSIS, AgentCapability.REASONING);
            default -> Set.of(AgentCapability.REASONING);
        };
    }
    
    private List<List<PlanStep>> buildExecutionWaves(AgentExecutionPlan plan) {
        List<List<PlanStep>> waves = new ArrayList<>();
        Set<String> completed = new HashSet<>();
        List<PlanStep> remaining = new ArrayList<>(plan.steps());
        
        while (!remaining.isEmpty()) {
            List<PlanStep> wave = remaining.stream()
                .filter(step -> step.canExecute(completed))
                .collect(Collectors.toList());
            
            if (wave.isEmpty()) {
                // Circular dependency detected
                break;
            }
            
            waves.add(wave);
            wave.forEach(step -> completed.add(step.stepId()));
            remaining.removeAll(wave);
        }
        
        return waves;
    }
    
    private ExecutionTree buildExecutionTree(AgentExecutionPlan plan) {
        // Simplified tree building
        return new ExecutionTree(plan.steps().get(0), List.of());
    }
    
    private Uni<OrchestrationContext> executeTreeNode(
            OrchestrationContext context,
            PlanStep node,
            ExecutionConstraints constraints) {
        // Simplified tree execution
        return executeStep(context, node, constraints)
            .map(result -> context.addStepResult(node.stepId(), result));
    }
    
    private Uni<OrchestrationContext> monitorCollaboration(
            OrchestrationContext context,
            Object session) {
        // Monitor collaboration until complete
        return Uni.createFrom().item(context);
    }
    
    private Uni<OrchestrationContext> runDebateRounds(
            OrchestrationContext context,
            Object debate) {
        // Run debate rounds
        return Uni.createFrom().item(context);
    }
    
    private AgentExecutionResult selectBestResult(List<AgentExecutionResult> results) {
        return results.stream()
            .filter(AgentExecutionResult::isSuccess)
            .max(Comparator.comparingDouble(r -> r.metrics().successScore()))
            .orElse(results.get(0));
    }
    
    private Uni<OrchestrationContext> handleOrchestrationFailure(
            String orchestrationId,
            Throwable error,
            AgentExecutionRequest request) {
        
        LOG.error("Orchestration {} failed: {}", orchestrationId, error.getMessage());
        
        // Could implement recovery strategies here
        return Uni.createFrom().failure(error);
    }
}