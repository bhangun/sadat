

@Path("/api/v1/tools")
@RegisterRestClient(configKey = "tool-gateway")
public interface ToolGatewayService {
    
    @GET
    @Path("/{toolName}/definition")
    Uni<ToolDefinition> getToolDefinition(@PathParam("toolName") String toolName);
    
    @POST
    @Path("/execute")
    Uni<ToolExecutionResponse> executeTool(ToolExecutionRequest request);
}