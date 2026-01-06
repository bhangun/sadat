
@Path("/api/v1/memory")
@RegisterRestClient(configKey = "memory-service")
public interface MemoryService {
    
    @POST
    @Path("/write")
    Uni<MemoryWriteResponse> writeMemory(MemoryWriteRequest request);
    
    @POST
    @Path("/query")
    Uni<MemoryQueryResponse> queryMemory(MemoryQueryRequest request);
    
    @POST
    @Path("/consolidate/{tenantId}")
    Uni<Void> consolidateMemories(@PathParam("tenantId") String tenantId);
}