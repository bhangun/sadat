@Value
@Builder
public class WorkflowDefinition {
    List<NodeInstance> nodes;
    List<Edge> edges;
    Map<String, Object> globalVariables;
    List<GuardrailPolicy> guardrails;
    WorkflowMetadata metadata;
}