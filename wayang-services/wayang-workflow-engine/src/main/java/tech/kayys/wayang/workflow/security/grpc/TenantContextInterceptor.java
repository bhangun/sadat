package tech.kayys.wayang.workflow.security.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.security.context.SecurityContextHolder;
import tech.kayys.wayang.workflow.security.context.SecurityContextHolder.SecurityContext;
import tech.kayys.wayang.workflow.security.model.ExecutionContext;

/**
 * Second interceptor in the chain.
 * Extracts tenant information and sets up the TenantContextHolder.
 * Enforces environment isolation (Prod cannot call Dev).
 */
@ApplicationScoped
@GlobalInterceptor
public class TenantContextInterceptor implements ServerInterceptor {

    private static final Logger LOG = Logger.getLogger(TenantContextInterceptor.class);

    @ConfigProperty(name = "wayang.environment", defaultValue = "production")
    String currentEnvironment;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        Context currentContext = Context.current();
        String principalType = AuthenticationInterceptor.PRINCIPAL_TYPE_KEY.get(currentContext);

        // 1. Handle Execution Context Tokens
        if (AuthenticationInterceptor.TYPE_EXECUTION_TOKEN.equals(principalType)) {
            ExecutionContext executionContext = AuthenticationInterceptor.EXECUTION_CONTEXT_KEY.get(currentContext);
            if (executionContext != null) {
                // Enforce Environment Isolation
                if (!currentEnvironment.equals(executionContext.environment())) {
                    LOG.errorf("Environment mismatch: Token env %s cannot access Current env %s",
                            executionContext.environment(), currentEnvironment);
                    call.close(Status.PERMISSION_DENIED.withDescription("Environment mismatch"), headers);
                    return new ServerCall.Listener<>() {
                    };
                }

                SecurityContextHolder.setContext(SecurityContext.fromExecutionContext(executionContext));
            }
        }
        // 2. Handle User JWTs or Service Accounts
        else if (AuthenticationInterceptor.TYPE_USER_JWT.equals(principalType)) {
            // For now we defer SecurityContextHolder population for Users to the specific
            // service
            // or rely on Quarkus SecurityIdentity downstream.
            // Ideally we populating it here requires extracting tenant from the unparsed
            // (or raw) token,
            // which we skipped in AuthInterceptor.
        }

        return Contexts.interceptCall(currentContext, call, headers, next);
    }
}
