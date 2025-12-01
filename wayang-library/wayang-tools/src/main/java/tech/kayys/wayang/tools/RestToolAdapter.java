
@ApplicationScoped
public class RestToolAdapter implements ToolAdapter {
    @Inject @RestClient
    WebClient webClient;
    
    @Override
    public ToolResult execute(ToolRequest request, ToolDescriptor tool) {
        Instant start = Instant.now();
        
        try {
            // Build request
            HttpRequest<?> httpRequest = buildHttpRequest(request, tool);
            
            // Execute
            HttpResponse<String> response = httpRequest.send(String.class);
            
            // Parse response
            Map<String, Object> output = parseResponse(
                response.body(),
                tool.getOutputSchema()
            );
            
            return ToolResult.builder()
                .requestId(request.getRequestId())
                .toolId(tool.getId())
                .status(Status.SUCCESS)
                .output(output)
                .executionTime(Duration.between(start, Instant.now()))
                .build();
                
        } catch (Exception e) {
            return ToolResult.builder()
                .requestId(request.getRequestId())
                .toolId(tool.getId())
                .status(Status.FAILED)
                .error(e.getMessage())
                .executionTime(Duration.between(start, Instant.now()))
                .build();
        }
    }
}