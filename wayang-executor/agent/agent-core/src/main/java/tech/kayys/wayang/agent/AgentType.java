package tech.kayys.wayang.agent;

import java.util.Set;

/**
 * ============================================================================
 * SILAT AGENT ORCHESTRATOR - DOMAIN MODEL
 * ============================================================================
 * 
 * Core domain entities for multi-agent orchestration system supporting:
 * - Common agents (task executors)
 * - Planner agents (planning and strategy)
 * - Coder agents (code generation/analysis)
 * - Analytics agents (data analysis)
 * - Orchestrator agents (meta-orchestration)
 * - Built-in sub-agents (planner, executor, evaluator)
 * 
 * Package: tech.kayys.silat.agent.domain
 */

// ==================== AGENT TYPE HIERARCHY ====================

/**
 * Base Agent Type - All agent types inherit from this
 */
public sealed interface AgentType 
    permits CommonAgent, PlannerAgent, CoderAgent, AnalyticsAgent, OrchestratorAgent {
    
    String typeName();
    Set<AgentCapability> requiredCapabilities();
    AgentRole role();
}