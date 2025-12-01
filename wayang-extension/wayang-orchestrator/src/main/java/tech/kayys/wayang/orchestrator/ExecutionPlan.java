package tech.kayys.wayang.orchestrator.model;

@Value
@Builder
public class ExecutionPlan {
    String planId;
    String version;
    UUID workflowId;
    List<NodeInstance> nodes;
    List<Edge> edges;
    Map<String, Object> globalVariables;
    PlanMetadata metadata;
    List<GuardrailPolicy> guardrails;
}