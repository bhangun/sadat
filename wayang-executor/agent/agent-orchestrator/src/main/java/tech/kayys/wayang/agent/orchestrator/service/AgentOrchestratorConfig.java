package tech.kayys.wayang.agent.orchestrator.service;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.AgentCapability;
import tech.kayys.wayang.agent.CommonAgent;
import tech.kayys.wayang.agent.PlannerAgent;
import tech.kayys.wayang.agent.PlanningStrategy;
import tech.kayys.wayang.agent.dto.AgentEndpoint;
import tech.kayys.wayang.agent.dto.EndpointType;

/**
 * Agent Orchestrator Configuration Bean
 */
@ApplicationScoped
public class AgentOrchestratorConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(AgentOrchestratorConfig.class);
    
    @Inject
    AgentRegistry agentRegistry;
    
    /**
     * Initialize orchestrator on startup
     */
    void onStart(@Observes StartupEvent event) {
        LOG.info("Initializing Agent Orchestrator...");
        
        // Register built-in agents
        registerBuiltInAgents();
        
        LOG.info("Agent Orchestrator initialized successfully");
    }
    
    /**
     * Register built-in agents (planner, executor, evaluator)
     */
    private void registerBuiltInAgents() {
        // Register built-in planner agent
        PlannerAgent plannerType = new PlannerAgent(
            PlanningStrategy.PLAN_AND_EXECUTE,
            3,
            true
        );
        
        agentRegistry.registerAgent(
            "built-in-planner",
            "Built-in Planner Agent",
            plannerType,
            Set.of(
                AgentCapability.PLANNING,
                AgentCapability.REASONING,
                AgentCapability.DECOMPOSITION
            ),
            new AgentEndpoint(EndpointType.INTERNAL, "internal://planner", Map.of()),
            "system"
        ).subscribe().with(
            reg -> LOG.info("Registered built-in planner agent"),
            error -> LOG.error("Failed to register planner", error)
        );
        
        // Register built-in executor agent
        CommonAgent executorType = new CommonAgent(
            "execution-coordinator",
            Set.of("coordination", "execution", "monitoring")
        );
        
        agentRegistry.registerAgent(
            "built-in-executor",
            "Built-in Executor Agent",
            executorType,
            Set.of(
                AgentCapability.COORDINATION,
                AgentCapability.TOOL_USE
            ),
            new AgentEndpoint(EndpointType.INTERNAL, "internal://executor", Map.of()),
            "system"
        ).subscribe().with(
            reg -> LOG.info("Registered built-in executor agent"),
            error -> LOG.error("Failed to register executor", error)
        );
        
        // Register built-in evaluator agent
        CommonAgent evaluatorType = new CommonAgent(
            "result-evaluator",
            Set.of("evaluation", "quality-assessment")
        );
        
        agentRegistry.registerAgent(
            "built-in-evaluator",
            "Built-in Evaluator Agent",
            evaluatorType,
            Set.of(
                AgentCapability.EVALUATION,
                AgentCapability.REASONING
            ),
            new AgentEndpoint(EndpointType.INTERNAL, "internal://evaluator", Map.of()),
            "system"
        ).subscribe().with(
            reg -> LOG.info("Registered built-in evaluator agent"),
            error -> LOG.error("Failed to register evaluator", error)
        );
    }
}
