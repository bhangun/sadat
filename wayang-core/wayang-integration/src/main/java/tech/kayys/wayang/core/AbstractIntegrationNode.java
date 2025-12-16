



import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import tech.kayys.wayang.nodes.AbstractNode;
import tech.kayys.wayang.nodes.NodeContext;
import tech.kayys.wayang.nodes.ExecutionResult;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Base class for all EIP/Camel integration nodes.
 * 
 * Responsibilities:
 * - Configure Camel routes based on node schema
 * - Map Camel errors to ErrorPayload
 * - Emit audit/provenance events
 * - Integrate with guardrails
 */
@RegisterForReflection
public abstract class AbstractIntegrationNode extends AbstractNode {
    
    @Inject
    protected CamelContext camelContext;
    
    @Inject
    protected IntegrationErrorHandler errorHandler;
    
    @Inject
    protected ProvenanceService provenanceService;
    
    /**
     * Main execution entry point - called by NodeExecutor
     */
    @Override
    public ExecutionResult execute(NodeContext ctx) {
        String routeId = getRouteId();
        
        try {
            // Ensure route is configured
            if (!camelContext.getRoute(routeId)) {
                RouteBuilder builder = createRouteBuilder(ctx);
                camelContext.addRoutes(builder);
            }
            
            // Send message to route
            Object output = camelContext.createProducerTemplate()
                .requestBody(getFromEndpoint(), ctx.getInput());
            
            // Record provenance
            provenanceService.record(ctx, "integration.success", output);
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            ErrorPayload error = errorHandler.handleCamelException(e, ctx);
            provenanceService.record(ctx, "integration.error", error);
            return ExecutionResult.error(error);
        }
    }
    
    /**
     * Create Camel RouteBuilder for this node
     */
    protected RouteBuilder createRouteBuilder(NodeContext ctx) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureRoute(this, ctx);
            }
        };
    }
    
    /**
     * Subclasses implement route configuration
     */
    protected abstract void configureRoute(RouteBuilder builder, NodeContext ctx);
    
    /**
     * Get Camel route ID from node config
     */
    protected String getRouteId() {
        return getConfig().getString("camelConfig.routeId");
    }
    
    /**
     * Get 'from' endpoint URI
     */
    protected String getFromEndpoint() {
        String from = getConfig().getString("camelConfig.from");
        return from != null ? from : "direct:" + getRouteId();
    }
    
    /**
     * Get error handler reference
     */
    protected String getErrorHandlerRef() {
        return getConfig().getString("camelConfig.errorHandlerRef", "defaultErrorHandler");
    }
}


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import tech.kayys.wayang.audit.ProvenanceService;
import tech.kayys.wayang.error.ErrorPayload;
import tech.kayys.wayang.integration.config.IntegrationNodeConfig;
import tech.kayys.wayang.integration.error.IntegrationErrorHandler;
import tech.kayys.wayang.nodes.AbstractNode;
import tech.kayys.wayang.nodes.ExecutionResult;
import tech.kayys.wayang.nodes.NodeContext;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all EIP/Camel integration nodes.
 * Provides common Camel route configuration and error handling.
 */
@Slf4j
@RegisterForReflection
public abstract class AbstractIntegrationNode extends AbstractNode {
    
    @Inject
    protected CamelContext camelContext;
    
    @Inject
    protected IntegrationErrorHandler errorHandler;
    
    @Inject
    protected ProvenanceService provenanceService;
    
    protected IntegrationNodeConfig integrationConfig;
    
    @Override
    protected void onLoad() {
        super.onLoad();
        this.integrationConfig = config.getConfigAs(IntegrationNodeConfig.class);
        
        try {
            // Register route if not already registered
            if (!isRouteRegistered()) {
                registerRoute();
            }
        } catch (Exception e) {
            log.error("Failed to register route for node {}", nodeId, e);
            throw new RuntimeException("Route registration failed", e);
        }
    }
    
    @Override
    public ExecutionResult execute(NodeContext context) {
        Instant startTime = Instant.now();
        String routeId = getRouteId();
        
        try {
            log.debug("Executing integration node {} with route {}", nodeId, routeId);
            
            // Prepare exchange headers with context metadata
            Map<String, Object> headers = prepareHeaders(context);
            
            // Execute route with timeout
            Object result = executeWithTimeout(context, headers);
            
            // Record provenance
            String provenanceRef = provenanceService.record(context, "integration.success", result);
            
            // Build successful result
            ExecutionResult execResult = ExecutionResult.success(result);
            execResult.setStartTime(startTime);
            execResult.setEndTime(Instant.now());
            execResult.getMetadata().put("routeId", routeId);
            execResult.getMetadata().put("provenanceRef", provenanceRef);
            
            return execResult;
            
        } catch (Exception e) {
            log.error("Error executing integration node {}", nodeId, e);
            
            ErrorPayload error = errorHandler.handleCamelException(e, context);
            error.setOriginNode(nodeId);
            error.setOriginRunId(context.getRunId());
            
            provenanceService.record(context, "integration.error", error);
            
            ExecutionResult execResult = ExecutionResult.error(error);
            execResult.setStartTime(startTime);
            execResult.setEndTime(Instant.now());
            
            return execResult;
        }
    }
    
    /**
     * Execute route with timeout protection
     */
    private Object executeWithTimeout(NodeContext context, Map<String, Object> headers) throws Exception {
        Integer timeoutMs = integrationConfig.getTimeout();
        
        if (timeoutMs == null || timeoutMs <= 0) {
            return executeCamelRoute(context, headers);
        }
        
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return executeCamelRoute(context, headers);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Execute the Camel route
     */
    private Object executeCamelRoute(NodeContext context, Map<String, Object> headers) {
        ProducerTemplate producer = camelContext.createProducerTemplate();
        
        try {
            Object body = context.getInput("body");
            String endpoint = getFromEndpoint();
            
            Exchange exchange = producer.request(endpoint, ex -> {
                ex.getIn().setBody(body);
                ex.getIn().setHeaders(headers);
                
                // Add context properties to exchange
                ex.setProperty("nodeContext", context);
                ex.setProperty("nodeId", nodeId);
                ex.setProperty("runId", context.getRunId());
            });
            
            // Check for exception in exchange
            if (exchange.getException() != null) {
                throw exchange.getException();
            }
            
            return exchange.getMessage().getBody();
            
        } finally {
            try {
                producer.stop();
            } catch (Exception e) {
                log.warn("Failed to stop producer template", e);
            }
        }
    }
    
    /**
     * Register Camel route
     */
    private void registerRoute() throws Exception {
        RouteBuilder builder = createRouteBuilder();
        camelContext.addRoutes(builder);
        log.info("Registered route {} for node {}", getRouteId(), nodeId);
    }
    
    /**
     * Check if route is already registered
     */
    private boolean isRouteRegistered() {
        return camelContext.getRoute(getRouteId()) != null;
    }
    
    /**
     * Create RouteBuilder for this node
     */
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                configureRoute(this);
            }
        };
    }
    
    /**
     * Subclasses implement route configuration
     */
    protected abstract void configureRoute(RouteBuilder builder) throws Exception;
    
    /**
     * Get Camel route ID
     */
    protected String getRouteId() {
        return integrationConfig.getCamelConfig().getRouteId();
    }
    
    /**
     * Get 'from' endpoint URI
     */
    protected String getFromEndpoint() {
        String from = integrationConfig.getCamelConfig().getFrom();
        return from != null ? from : "direct:" + getRouteId();
    }
    
    /**
     * Get error handler reference
     */
    protected String getErrorHandlerRef() {
        return integrationConfig.getCamelConfig().getErrorHandlerRef();
    }
    
    /**
     * Prepare headers from context
     */
    protected Map<String, Object> prepareHeaders(NodeContext context) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("nodeId", nodeId);
        headers.put("runId", context.getRunId());
        headers.put("workflowId", context.getWorkflowId());
        headers.put("tenantId", context.getTenantId());
        headers.put("traceId", context.getTraceId());
        
        // Add custom headers from context metadata
        if (context.getMetadata() != null) {
            context.getMetadata().forEach((key, value) -> {
                if (key.startsWith("header.")) {
                    headers.put(key.substring(7), value);
                }
            });
        }
        
        return headers;
    }
    
    @Override
    protected void onUnload() {
        super.onUnload();
        
        try {
            // Remove route when node is unloaded
            if (isRouteRegistered()) {
                camelContext.getRouteController().stopRoute(getRouteId());
                camelContext.removeRoute(getRouteId());
                log.info("Removed route {} for node {}", getRouteId(), nodeId);
            }
        } catch (Exception e) {
            log.error("Failed to remove route for node {}", nodeId, e);
        }
    }
}