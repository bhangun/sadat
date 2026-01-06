package tech.kayys.wayang.agent.orchestrator.model;

import java.util.List;

import tech.kayys.wayang.agent.dto.PlanStep;

/**
 * Execution tree structure
 */
record ExecutionTree(PlanStep root, List<ExecutionTree> children) {}

