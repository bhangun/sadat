
@ApplicationScoped
@Path("/api")
public class WayangAPIGateway {
    @Inject AuthenticationFilter authFilter;
    @Inject AuthorizationFilter authzFilter;
    @Inject RateLimiter rateLimiter;
    @Inject RequestValidator requestValidator;
    @Inject CircuitBreakerRegistry circuitBreakerRegistry;
    @Inject MetricsCollector metricsCollector;
    
    @POST
    @Path("/workflows")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createWorkflow(
        @Context SecurityContext securityContext,
        CreateWorkflowRequest request
    ) {
        return handleRequest(securityContext, request, () -> {
            // Delegate to Designer Service
            Workflow workflow = designerService.createWorkflow(request);
            return Response.ok(workflow).build();
        });
    }
    
    @POST
    @Path("/workflows/{workflowId}/execute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeWorkflow(
        @Context SecurityContext securityContext,
        @PathParam("workflowId") UUID workflowId,
        ExecutionRequest request
    ) {
        return handleRequest(securityContext, request, () -> {
            // Delegate to Orchestrator
            CompletableFuture<ExecutionRun> future = 
                orchestrator.execute(workflowId, request);
            
            ExecutionRun run = future.join();
            return Response.accepted(run).build();
        });
    }
    
    private Response handleRequest(
        SecurityContext securityContext,
        Object request,
        Supplier<Response> handler
    ) {
        Span span = startSpan("api-request");
        
        try {
            // Authentication
            Principal principal = securityContext.getUserPrincipal();
            if (principal == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            // Validate request
            ValidationResult validation = requestValidator.validate(request);
            if (!validation.isValid()) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(validation.getErrors())
                    .build();
            }
            
            // Extract tenant
            UUID tenantId = extractTenantId(principal);
            
            // Rate limiting
            if (!rateLimiter.allowRequest(tenantId)) {
                return Response
                    .status(Response.Status.TOO_MANY_REQUESTS)
                    .build();
            }
            
            // Authorization
            if (!authzFilter.authorize(principal, request)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            
            // Execute with circuit breaker
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.get("api");
            
            Response response = circuitBreaker.executeSupplier(handler);
            
            // Collect metrics
            metricsCollector.recordRequest(
                request.getClass().getSimpleName(),
                response.getStatus()
            );
            
            return response;
            
        } catch (Exception e) {
            logger.error("API request failed", e);
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } finally {
            span.end();
        }
    }
}