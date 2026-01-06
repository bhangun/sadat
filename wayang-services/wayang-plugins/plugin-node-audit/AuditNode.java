
/**
 * Audit Node - Log events for compliance and tracing
 * Provides tamper-proof execution logs with optional signing
 */
@ApplicationScoped
@NodeType("builtin.audit")
public class AuditNode extends AbstractNode {
    
    @Inject
    AuditService auditService;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var event = context.getInput("event");
        var level = config.getString("level", "INFO");
        var tags = config.getList("tags", String.class);
        
        var auditEntry = AuditEntry.builder()
            .event(event)
            .level(AuditLevel.valueOf(level))
            .tags(tags)
            .runId(context.getRunId())
            .nodeId(context.getNodeId())
            .tenantId(context.getTenantId())
            .timestamp(Instant.now())
            .metadata(context.getMetadata().toMap())
            .build();
        
        return auditService.log(auditEntry)
            .map(auditId -> ExecutionResult.success(Map.of(
                "auditId", auditId,
                "level", level
            )));
    }
}