package tech.kayys.wayang.agent.orchestrator.service;


import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.PlanningStrategy;
import tech.kayys.wayang.agent.dto.AgentExecutionPlan;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentExecutionResult;
import tech.kayys.wayang.agent.dto.ExecutionError;
import tech.kayys.wayang.agent.dto.PlanMetadata;
import tech.kayys.wayang.agent.dto.PlanStep;
import tech.kayys.wayang.agent.dto.StepStatus;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * ORCHESTRATOR PLANNER
 * ============================================================================
 * 
 * Built-in planner agent responsible for:
 * - Creating execution plans from high-level tasks
 * - Task decomposition and dependency analysis
 * - Agent selection and task assignment
 * - Adaptive replanning when failures occur
 * 
 * Supports multiple planning strategies:
 * - Hierarchical decomposition
 * - Chain-of-thought reasoning
 * - Plan-and-execute
 * - Adaptive planning
 */
@ApplicationScoped
public class OrchestratorPlanner {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrchestratorPlanner.class);
    
    @Inject
    AgentRegistry agentRegistry;
    
    @Inject
    LLMService llmService;
    
    /**
     * Create execution plan from request
     */
    public Uni<AgentExecutionPlan> createPlan(AgentExecutionRequest request) {
        LOG.info("Creating execution plan for: {}", request.taskDescription());
        
        // Analyze task complexity
        return analyzeTask(request)
            .flatMap(analysis -> {
                // Choose planning strategy based on analysis
                PlanningStrategy strategy = selectStrategy(analysis);
                
                // Generate plan using selected strategy
                return generatePlan(request, analysis, strategy);
            });
    }
    
    /**
     * Replan when step fails
     */
    public Uni<AgentExecutionPlan> replan(
            AgentExecutionPlan originalPlan,
            PlanStep failedStep,
            AgentExecutionResult failureResult,
            Map<String, AgentExecutionResult> completedSteps) {
        
        LOG.info("Replanning after step {} failure", failedStep.stepId());
        
        // Analyze failure
        return analyzeFailure(failedStep, failureResult)
            .flatMap(failureAnalysis -> {
                // Generate alternative approach
                return generateAlternativePlan(
                    originalPlan,
                    failedStep,
                    failureAnalysis,
                    completedSteps
                );
            });
    }
    
    /**
     * Analyze task to determine complexity and requirements
     */
    private Uni<TaskAnalysis> analyzeTask(AgentExecutionRequest request) {
        LOG.debug("Analyzing task complexity");
        
        return Uni.createFrom().item(() -> {
            // Simple heuristic-based analysis
            // In production, this would use LLM or ML model
            
            TaskComplexity complexity = determineComplexity(request.taskDescription());
            Set<String> requiredSkills = extractRequiredSkills(request);
            int estimatedSteps = estimateSteps(complexity);
            boolean requiresSpecialization = 
                request.requiredCapabilities().size() > 2;
            
            return new TaskAnalysis(
                complexity,
                requiredSkills,
                estimatedSteps,
                requiresSpecialization,
                Map.of()
            );
        });
    }
    
    /**
     * Select planning strategy based on task analysis
     */
    private PlanningStrategy selectStrategy(TaskAnalysis analysis) {
        if (analysis.complexity() == TaskComplexity.HIGH) {
            return PlanningStrategy.HIERARCHICAL;
        } else if (analysis.requiresSpecialization()) {
            return PlanningStrategy.PLAN_AND_EXECUTE;
        } else {
            return PlanningStrategy.CHAIN_OF_THOUGHT;
        }
    }
    
    /**
     * Generate execution plan
     */
    private Uni<AgentExecutionPlan> generatePlan(
            AgentExecutionRequest request,
            TaskAnalysis analysis,
            PlanningStrategy strategy) {
        
        LOG.debug("Generating plan using strategy: {}", strategy);
        
        return switch (strategy) {
            case HIERARCHICAL -> generateHierarchicalPlan(request, analysis);
            case CHAIN_OF_THOUGHT -> generateChainOfThoughtPlan(request, analysis);
            case PLAN_AND_EXECUTE -> generatePlanAndExecutePlan(request, analysis);
            case REACT -> generateReActPlan(request, analysis);
            default -> generateSimplePlan(request, analysis);
        };
    }
    
    /**
     * Generate hierarchical plan with task decomposition
     */
    private Uni<AgentExecutionPlan> generateHierarchicalPlan(
            AgentExecutionRequest request,
            TaskAnalysis analysis) {
        
        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = new ArrayList<>();
        
        // Step 1: High-level decomposition
        steps.add(new PlanStep(
            "decompose",
            "Decompose task into subtasks",
            "PLANNER",
            Map.of("task", request.taskDescription()),
            Set.of(),
            StepStatus.PENDING,
            null
        ));
        
        // Step 2-N: Execute subtasks
        for (int i = 0; i < analysis.estimatedSteps(); i++) {
            steps.add(new PlanStep(
                "execute-" + i,
                "Execute subtask " + (i + 1),
                determineAgentType(analysis.requiredSkills()),
                Map.of(),
                Set.of("decompose"),
                StepStatus.PENDING,
                null
            ));
        }
        
        // Final step: Aggregate results
        steps.add(new PlanStep(
            "aggregate",
            "Aggregate and synthesize results",
            "COMMON_AGENT",
            Map.of(),
            steps.stream()
                .filter(s -> s.stepId().startsWith("execute-"))
                .map(PlanStep::stepId)
                .collect(Collectors.toSet()),
            StepStatus.PENDING,
            null
        ));
        
        PlanMetadata metadata = new PlanMetadata(
            strategy,
            steps.size(),
            analysis.estimatedSteps() * 10000, // rough estimate
            0.8,
            Map.of("complexity", analysis.complexity().name())
        );
        
        return Uni.createFrom().item(new AgentExecutionPlan(
            planId,
            "Hierarchical plan for: " + request.taskDescription(),
            steps,
            new HashMap<>(request.context()),
            metadata,
            Instant.now()
        ));
    }
    
    /**
     * Generate chain-of-thought plan
     */
    private Uni<AgentExecutionPlan> generateChainOfThoughtPlan(
            AgentExecutionRequest request,
            TaskAnalysis analysis) {
        
        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = new ArrayList<>();
        
        // Use LLM to generate reasoning chain
        return llmService.generateReasoningChain(request.taskDescription())
            .map(reasoningSteps -> {
                Set<String> previousStepIds = new HashSet<>();
                
                for (int i = 0; i < reasoningSteps.size(); i++) {
                    String stepId = "reason-" + i;
                    steps.add(new PlanStep(
                        stepId,
                        reasoningSteps.get(i),
                        "COMMON_AGENT",
                        Map.of("reasoning", reasoningSteps.get(i)),
                        new HashSet<>(previousStepIds),
                        StepStatus.PENDING,
                        null
                    ));
                    previousStepIds.add(stepId);
                }
                
                PlanMetadata metadata = new PlanMetadata(
                    PlanningStrategy.CHAIN_OF_THOUGHT,
                    steps.size(),
                    steps.size() * 5000,
                    0.85,
                    Map.of()
                );
                
                return new AgentExecutionPlan(
                    planId,
                    "Chain-of-thought plan",
                    steps,
                    new HashMap<>(request.context()),
                    metadata,
                    Instant.now()
                );
            });
    }
    
    /**
     * Generate plan-and-execute plan
     */
    private Uni<AgentExecutionPlan> generatePlanAndExecutePlan(
            AgentExecutionRequest request,
            TaskAnalysis analysis) {
        
        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = List.of(
            // Planning phase
            new PlanStep(
                "detailed-planning",
                "Create detailed execution plan",
                "PLANNER",
                Map.of("task", request.taskDescription()),
                Set.of(),
                StepStatus.PENDING,
                null
            ),
            // Execution phase
            new PlanStep(
                "execute-plan",
                "Execute the detailed plan",
                determineAgentType(analysis.requiredSkills()),
                Map.of(),
                Set.of("detailed-planning"),
                StepStatus.PENDING,
                null
            ),
            // Verification phase
            new PlanStep(
                "verify-results",
                "Verify execution results",
                "COMMON_AGENT",
                Map.of(),
                Set.of("execute-plan"),
                StepStatus.PENDING,
                null
            )
        );
        
        PlanMetadata metadata = new PlanMetadata(
            PlanningStrategy.PLAN_AND_EXECUTE,
            steps.size(),
            30000,
            0.9,
            Map.of()
        );
        
        return Uni.createFrom().item(new AgentExecutionPlan(
            planId,
            "Plan-and-execute for: " + request.taskDescription(),
            steps,
            new HashMap<>(request.context()),
            metadata,
            Instant.now()
        ));
    }
    
    /**
     * Generate ReAct (Reasoning + Acting) plan
     */
    private Uni<AgentExecutionPlan> generateReActPlan(
            AgentExecutionRequest request,
            TaskAnalysis analysis) {
        
        String planId = UUID.randomUUID().toString();
        
        // ReAct is more dynamic - create initial observation step
        List<PlanStep> steps = List.of(
            new PlanStep(
                "observe",
                "Observe current state and analyze task",
                "COMMON_AGENT",
                Map.of("task", request.taskDescription()),
                Set.of(),
                StepStatus.PENDING,
                null
            ),
            new PlanStep(
                "reason-act-cycle",
                "Iterative reasoning and action cycle",
                "COMMON_AGENT",
                Map.of("maxCycles", 5),
                Set.of("observe"),
                StepStatus.PENDING,
                null
            )
        );
        
        PlanMetadata metadata = new PlanMetadata(
            PlanningStrategy.REACT,
            steps.size(),
            50000,
            0.75,
            Map.of("adaptive", "true")
        );
        
        return Uni.createFrom().item(new AgentExecutionPlan(
            planId,
            "ReAct plan",
            steps,
            new HashMap<>(request.context()),
            metadata,
            Instant.now()
        ));
    }
    
    /**
     * Generate simple sequential plan
     */
    private Uni<AgentExecutionPlan> generateSimplePlan(
            AgentExecutionRequest request,
            TaskAnalysis analysis) {
        
        String planId = UUID.randomUUID().toString();
        
        List<PlanStep> steps = List.of(
            new PlanStep(
                "execute-task",
                request.taskDescription(),
                determineAgentType(analysis.requiredSkills()),
                new HashMap<>(request.context()),
                Set.of(),
                StepStatus.PENDING,
                null
            )
        );
        
        PlanMetadata metadata = new PlanMetadata(
            PlanningStrategy.ADAPTIVE,
            steps.size(),
            10000,
            0.9,
            Map.of()
        );
        
        return Uni.createFrom().item(new AgentExecutionPlan(
            planId,
            "Simple plan",
            steps,
            new HashMap<>(request.context()),
            metadata,
            Instant.now()
        ));
    }
    
    /**
     * Analyze step failure
     */
    private Uni<FailureAnalysis> analyzeFailure(
            PlanStep failedStep,
            AgentExecutionResult result) {
        
        return Uni.createFrom().item(() -> {
            FailureType type = categorizeFailure(result.errors());
            boolean isRecoverable = determineRecoverability(type, result);
            List<String> alternativeApproaches = 
                suggestAlternatives(failedStep, result);
            
            return new FailureAnalysis(
                type,
                isRecoverable,
                alternativeApproaches,
                result.errors()
            );
        });
    }
    
    /**
     * Generate alternative plan after failure
     */
    private Uni<AgentExecutionPlan> generateAlternativePlan(
            AgentExecutionPlan originalPlan,
            PlanStep failedStep,
            FailureAnalysis analysis,
            Map<String, AgentExecutionResult> completedSteps) {
        
        if (!analysis.isRecoverable()) {
            return Uni.createFrom().failure(
                new IllegalStateException("Failure is not recoverable")
            );
        }
        
        // Create new plan incorporating alternative approaches
        List<PlanStep> newSteps = new ArrayList<>();
        
        // Keep completed steps
        originalPlan.steps().stream()
            .filter(step -> completedSteps.containsKey(step.stepId()))
            .forEach(newSteps::add);
        
        // Add alternative steps for failed step
        for (String alternative : analysis.alternativeApproaches()) {
            newSteps.add(new PlanStep(
                "alt-" + UUID.randomUUID().toString(),
                alternative,
                determineAlternativeAgentType(failedStep),
                failedStep.stepContext(),
                failedStep.dependencies(),
                StepStatus.PENDING,
                null
            ));
        }
        
        // Add remaining steps
        originalPlan.steps().stream()
            .filter(step -> !completedSteps.containsKey(step.stepId()))
            .filter(step -> !step.stepId().equals(failedStep.stepId()))
            .forEach(newSteps::add);
        
        PlanMetadata metadata = new PlanMetadata(
            PlanningStrategy.ADAPTIVE,
            newSteps.size(),
            newSteps.size() * 10000,
            0.7,
            Map.of("replanned", "true", "failedStep", failedStep.stepId())
        );
        
        return Uni.createFrom().item(new AgentExecutionPlan(
            UUID.randomUUID().toString(),
            "Replanned: " + originalPlan.description(),
            newSteps,
            originalPlan.planContext(),
            metadata,
            Instant.now()
        ));
    }
    
    // ==================== HELPER METHODS ====================
    
    private TaskComplexity determineComplexity(String description) {
        int words = description.split("\\s+").length;
        if (words > 50) return TaskComplexity.HIGH;
        if (words > 20) return TaskComplexity.MEDIUM;
        return TaskComplexity.LOW;
    }
    
    private Set<String> extractRequiredSkills(AgentExecutionRequest request) {
        Set<String> skills = new HashSet<>();
        
        // Extract from task description keywords
        String desc = request.taskDescription().toLowerCase();
        if (desc.contains("code") || desc.contains("program")) {
            skills.add("CODING");
        }
        if (desc.contains("analyze") || desc.contains("data")) {
            skills.add("ANALYTICS");
        }
        if (desc.contains("plan") || desc.contains("strategy")) {
            skills.add("PLANNING");
        }
        
        return skills.isEmpty() ? Set.of("GENERAL") : skills;
    }
    
    private int estimateSteps(TaskComplexity complexity) {
        return switch (complexity) {
            case LOW -> 1;
            case MEDIUM -> 3;
            case HIGH -> 5;
        };
    }
    
    private String determineAgentType(Set<String> skills) {
        if (skills.contains("CODING")) return "CODER_AGENT";
        if (skills.contains("ANALYTICS")) return "ANALYTICS_AGENT";
        if (skills.contains("PLANNING")) return "PLANNER_AGENT";
        return "COMMON_AGENT";
    }
    
    private String determineAlternativeAgentType(PlanStep failedStep) {
        // Try different agent type
        return switch (failedStep.assignedAgentType()) {
            case "COMMON_AGENT" -> "PLANNER_AGENT";
            case "CODER_AGENT" -> "COMMON_AGENT";
            default -> "COMMON_AGENT";
        };
    }
    
    private FailureType categorizeFailure(List<ExecutionError> errors) {
        if (errors.stream().anyMatch(e -> e.errorCode().contains("TIMEOUT"))) {
            return FailureType.TIMEOUT;
        }
        if (errors.stream().anyMatch(e -> e.errorCode().contains("CAPABILITY"))) {
            return FailureType.INSUFFICIENT_CAPABILITY;
        }
        return FailureType.EXECUTION_ERROR;
    }
    
    private boolean determineRecoverability(
            FailureType type,
            AgentExecutionResult result) {
        return type != FailureType.FATAL_ERROR;
    }
    
    private List<String> suggestAlternatives(
            PlanStep failedStep,
            AgentExecutionResult result) {
        return List.of(
            "Retry with different agent",
            "Break down into smaller steps",
            "Use fallback approach"
        );
    }
}
