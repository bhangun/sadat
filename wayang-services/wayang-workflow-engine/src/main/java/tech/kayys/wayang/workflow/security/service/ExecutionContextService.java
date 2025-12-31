package tech.kayys.wayang.workflow.security.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.security.model.ExecutionContext;
import tech.kayys.wayang.workflow.security.model.Initiator;
import tech.kayys.wayang.workflow.security.model.Initiator.InitiatorType;
import tech.kayys.wayang.workflow.security.model.Permission;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for creating and validating execution context tokens.
 * These tokens are used internally by the execution plane for secure workflow
 * execution.
 */
@ApplicationScoped
public class ExecutionContextService {

    private static final Logger LOG = Logger.getLogger(ExecutionContextService.class);

    @Inject
    JWTParser jwtParser;

    @ConfigProperty(name = "wayang.security.jwt.issuer", defaultValue = "wayang-workflow-engine")
    String issuer;

    @ConfigProperty(name = "wayang.security.jwt.ttl-seconds", defaultValue = "3600")
    long ttlSeconds;

    /**
     * Create an execution context from a workflow run and initiator
     */
    public ExecutionContext createContext(WorkflowRun run, Initiator initiator) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofSeconds(ttlSeconds));

        // Define default permissions for workflow execution based on initiator roles
        Set<Permission> permissions = new HashSet<>();

        // Base permissions for all executions
        permissions
                .add(Permission.builder().resource("workflow").action("execute").target(run.getWorkflowId()).build());
        permissions.add(Permission.builder().resource("node").action("execute").target("*").build());
        permissions.add(Permission.builder().resource("state").action("read").target(run.getRunId()).build());
        permissions.add(Permission.builder().resource("state").action("write").target(run.getRunId()).build());

        // Add role-based permissions (example logic)
        if (initiator.roles() != null && initiator.roles().contains("admin")) {
            permissions.add(Permission.builder().resource("*").action("*").target("*").build());
        }

        return new ExecutionContext(
                run.getRunId(),
                run.getWorkflowId(),
                run.getTenantId(),
                "production", // TODO: Get from configuration
                initiator,
                permissions,
                now,
                expiry);
    }

    /**
     * Sign an execution context into a JWT token
     */
    public String signContext(ExecutionContext context) {
        context.validate();

        Set<String> permissionStrings = context.permissions().stream()
                .map(Permission::toString)
                .collect(Collectors.toSet());

        return Jwt.issuer(issuer)
                .upn(context.initiator().userId())
                .groups(context.initiator().roles())
                .claim("runId", context.runId())
                .claim("workflowId", context.workflowId())
                .claim("tenantId", context.tenantId())
                .claim("environment", context.environment())
                .claim("initiatorType", context.initiator().type().name())
                .claim("permissions", permissionStrings)
                .issuedAt(context.issuedAt())
                .expiresAt(context.expiresAt())
                .sign();
    }

    /**
     * Validate and parse an execution context token
     */
    public Uni<ExecutionContext> validateContext(String token) {
        return Uni.createFrom().item(() -> {
            try {
                // Parse directly (synchronous)
                JsonWebToken jwt = jwtParser.parse(token);

                // Extract claims
                String runId = jwt.getClaim("runId");
                String workflowId = jwt.getClaim("workflowId");
                String tenantId = jwt.getClaim("tenantId");
                String environment = jwt.getClaim("environment");
                String userId = jwt.getName();
                String initiatorTypeStr = jwt.getClaim("initiatorType");

                InitiatorType initiatorType = initiatorTypeStr != null
                        ? InitiatorType.valueOf(initiatorTypeStr)
                        : InitiatorType.USER;

                @SuppressWarnings("unchecked")
                Set<String> roles = new HashSet<>(jwt.getGroups());

                @SuppressWarnings("unchecked")
                Set<String> permissionStrings = jwt.claim("permissions")
                        .map(p -> new HashSet<>((java.util.Collection<String>) p))
                        .orElse(new HashSet<>());

                Set<Permission> permissions = permissionStrings.stream()
                        .map(Permission::fromString)
                        .collect(Collectors.toSet());

                Initiator initiator = new Initiator(initiatorType, userId, roles);

                ExecutionContext context = new ExecutionContext(
                        runId,
                        workflowId,
                        tenantId,
                        environment,
                        initiator,
                        permissions,
                        Instant.ofEpochSecond(jwt.getIssuedAtTime()),
                        Instant.ofEpochSecond(jwt.getExpirationTime()));

                context.validate();
                return context;

            } catch (ParseException e) {
                LOG.errorf(e, "Failed to parse execution context token");
                throw new SecurityException("Invalid execution context token", e);
            }
        });
    }

    /**
     * Check if the execution context has a specific permission
     */
    public boolean checkPermission(ExecutionContext context, Permission permission) {
        if (context == null) {
            return false;
        }
        return context.hasPermission(permission);
    }

    /**
     * Enforce that the execution context has a specific permission
     */
    public void requirePermission(ExecutionContext context, Permission permission) {
        if (!checkPermission(context, permission)) {
            throw new SecurityException("Permission denied: " + permission);
        }
    }
}
