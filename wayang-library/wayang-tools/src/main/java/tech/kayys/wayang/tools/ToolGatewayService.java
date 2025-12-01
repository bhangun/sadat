package tech.kayys.wayang.tools.service;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.tools.mcp.*;
import tech.kayys.wayang.tools.registry.ToolRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ToolGatewayService {
    
    private static final Logger LOG = Logger.getLogger(ToolGatewayService.class);
    
    @Inject
    ToolRegistry toolRegistry;
    
    @Inject
    ToolValidator toolValidator;
    
    @Inject
    SecretManager secretManager;
    
    @Inject
    RateLimiter rateLimiter;
    
    @Inject
    AuditService auditService;
    
    public Uni<ToolResponse> executeTool(ToolRequest request, ExecutionContext context) {
        LOG.infof("Executing tool: %s", request.toolName());
        
        return toolRegistry.getTool(request.toolName())
            .flatMap(tool -> {
                // Validate request against tool schema
                return toolValidator.validate(request, tool.schema())
                    .flatMap(validationResult -> {
                        if (!validationResult.isValid()) {
                            return Uni.createFrom().item(
                                ToolResponse.error(ErrorPayload.validationError(
                                    validationResult.errors()
                                ))
                            );
                        }
                        
                        // Check rate limits
                        return rateLimiter.checkLimit(context.tenantId(), request.toolName())
                            .flatMap(allowed -> {
                                if (!allowed) {
                                    return Uni.createFrom().item(
                                        ToolResponse.error(ErrorPayload.rateLimitError())
                                    );
                                }
                                
                                // Inject secrets if needed
                                return injectSecrets(request, tool, context)
                                    .flatMap(enrichedRequest -> 
                                        executeWithAudit(enrichedRequest, tool, context)
                                    );
                            });
                    });
            });
    }
    
    private Uni<ToolRequest> injectSecrets(
        ToolRequest request, 
        MCPTool tool, 
        ExecutionContext context
    ) {
        if (tool.requiredSecrets().isEmpty()) {
            return Uni.createFrom().item(request);
        }
        
        return secretManager.getSecrets(context.tenantId(), tool.requiredSecrets())
            .map(secrets -> request.withSecrets(secrets));
    }
    
    private Uni<ToolResponse> executeWithAudit(
        ToolRequest request, 
        MCPTool tool, 
        ExecutionContext context
    ) {
        long startTime = System.currentTimeMillis();
        
        return tool.execute(request)
            .invoke(response -> {
                long duration = System.currentTimeMillis() - startTime;
                
                // Audit the tool call
                auditService.auditToolCall(AuditPayload.builder()
                    .event("TOOL_EXECUTED")
                    .runId(context.runId())
                    .nodeId(context.nodeId())
                    .actor(AuditPayload.Actor.system())
                    .metadata(Map.of(
                        "toolName", request.toolName(),
                        "durationMs", duration,
                        "status", response.status()
                    ))
                    .build()
                );
            })
            .onFailure().recoverWithItem(failure -> 
                ToolResponse.error(ErrorPayload.toolError(failure))
            );
    }
}