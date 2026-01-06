@Path("/api/v1/hitl")
@RegisterRestClient(configKey = "hitl-service")
public interface HITLServiceClient {
    
    @POST
    @Path("/tasks")
    Uni<HITLTaskResponse> createTask(HITLTaskRequest request);
    
    @GET
    @Path("/tasks/{taskId}/status")
    Uni<HITLTaskStatus> getTaskStatus(@PathParam("taskId") String taskId);
    
    @GET
    @Path("/tasks/{taskId}/decision")
    Uni<HITLDecision> getTaskDecision(@PathParam("taskId") String taskId);
}
