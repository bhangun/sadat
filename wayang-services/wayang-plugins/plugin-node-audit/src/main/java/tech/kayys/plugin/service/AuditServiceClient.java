
@Path("/api/v1/audit")
@RegisterRestClient(configKey = "audit-service")
public interface AuditServiceClient {
    
    @POST
    @Path("/log")
    Uni<AuditLogResponse> logEntry(AuditLogRequest request);
    
    @POST
    @Path("/query")
    Uni<AuditQueryResponse> queryLogs(AuditQuery query);
}
