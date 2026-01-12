package tech.kayys.wayang.resources;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.security.domain.SecurityAuditLog;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.IketSecurityService;

@Path("/api/v1/audit-logs")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Audit Logs", description = "Security audit logs")
@SecurityRequirement(name = "bearer-jwt")
@RolesAllowed({ "admin", "security_auditor" })
public class AuditLogResource {

    @Inject
    IketSecurityService iketSecurity;

    @GET
    @Operation(summary = "Query audit logs")
    public Uni<List<SecurityAuditLog>> queryLogs(
            @QueryParam("action") String action,
            @QueryParam("resource_type") String resourceType,
            @QueryParam("from") Long fromTimestamp,
            @QueryParam("to") Long toTimestamp,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {

        AuthenticatedUser user = iketSecurity.getCurrentUser();

        // Build query
        StringBuilder query = new StringBuilder("tenantId = ?1");
        List<Object> params = new ArrayList<>();
        params.add(user.tenantId());

        if (action != null) {
            query.append(" and action = ?").append(params.size() + 1);
            params.add(action);
        }

        if (resourceType != null) {
            query.append(" and resourceType = ?").append(params.size() + 1);
            params.add(resourceType);
        }

        query.append(" order by timestamp desc");

        return SecurityAuditLog.find(query.toString(), params.toArray())
                .page(page, size)
                .list();
    }
}